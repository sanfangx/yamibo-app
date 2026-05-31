package me.thenano.yamibo.yamibo_app.repository.sign

import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.SignActionResult
import io.github.littlesurvival.dto.page.SignActionStatus
import io.github.littlesurvival.dto.page.SignPage
import io.github.littlesurvival.parse.SignPageParser
import kotlinx.coroutines.runBlocking
import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.i18n.AppMessage
import me.thenano.yamibo.yamibo_app.repository.AuthRepository
import me.thenano.yamibo.yamibo_app.repository.SignRepository
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.util.time.currentLocalDateKey
import me.thenano.yamibo.yamibo_app.util.time.currentLocalDateKeyAt
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import me.thenano.yamibo.yamiboapp.SignDailyRecord

class SignRepositoryImpl(
    db: Database,
    private val authRepository: AuthRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val yamiboClient: YamiboClient,
) : SignRepository {
    private val recordQueries = db.signDailyRecordQueries
    private val signPageParser = SignPageParser()

    override suspend fun fetchPageInfo(): YamiboResult<SignRepository.SignPageInfo> {
        if (!authRepository.isLoggedIn()) return YamiboResult.NotLoggedIn
        val cookie = authRepository.cookieStore.load().orEmpty()

        return when (val result = yamiboClient.fetchSignPage(cookie)) {
            is YamiboResult.Success -> {
                val info = result.value.toAppModel()
                updateTodayRecord(info)
                YamiboResult.Success(info)
            }

            is YamiboResult.Failure ->
                YamiboResult.Failure(toFriendlyFailure(result.reason), result.exception)

            is YamiboResult.NotLoggedIn -> result
            is YamiboResult.NoPermission -> result
            is YamiboResult.Maintenance -> result
        }
    }

    override suspend fun runAutoSign(allowRepair: Boolean): YamiboResult<SignRepository.ActionResult> {
        val initialInfo = getCachedPageInfo() ?: when (val result = fetchPageInfo()) {
            is YamiboResult.Success -> result.value
            is YamiboResult.NotLoggedIn -> return result
            is YamiboResult.NoPermission -> return result
            is YamiboResult.Maintenance -> return result
            is YamiboResult.Failure -> return result
        }

        var pageInfo = initialInfo
        var repairCount = 0
        var lastMessage: String
        var finalStatus = if (pageInfo.hasSignedToday) {
            SignRepository.ActionStatus.ALREADY_SIGNED
        } else {
            SignRepository.ActionStatus.SUCCESS
        }

        if (!pageInfo.hasSignedToday) {
            val signUrl = pageInfo.signActionUrl
                ?: return YamiboResult.Failure(msg("sign.verified_no_button"))
            val signAction = when (val action = executeAction(signUrl)) {
                is YamiboResult.Success -> action.value
                is YamiboResult.NotLoggedIn -> return action
                is YamiboResult.NoPermission -> return action
                is YamiboResult.Maintenance -> return action
                is YamiboResult.Failure -> return action
            }
            lastMessage = signAction.message
            finalStatus = signAction.status

            pageInfo = when (val refreshed = fetchPageInfo()) {
                is YamiboResult.Success -> {
                    if (signAction.status.isTodaySignedStatus()) {
                        optimisticSignedPageInfo(refreshed.value)
                    } else {
                        refreshed.value
                    }
                }
                else -> optimisticSignedPageInfo(pageInfo)
            }
        } else {
            lastMessage = msg("sign.already_signed")
        }

        if (allowRepair) {
            var seenRepairValues = mutableSetOf<String>()
            while (pageInfo.repairOptions.isNotEmpty()) {
                val prefix = pageInfo.repairActionPrefix ?: break
                val repairOption = pageInfo.repairOptions.firstOrNull { it.value !in seenRepairValues } ?: break
                seenRepairValues += repairOption.value

                val repairUrl = buildAbsoluteUrl("$prefix${repairOption.value}")
                val repairAction = when (val action = executeAction(repairUrl)) {
                    is YamiboResult.Success -> action.value
                    is YamiboResult.NotLoggedIn -> return action
                    is YamiboResult.NoPermission -> return action
                    is YamiboResult.Maintenance -> return action
                    is YamiboResult.Failure -> return action
                }
                repairCount += 1
                lastMessage = repairAction.message
                finalStatus = repairAction.status

                pageInfo = when (val refreshed = fetchPageInfo()) {
                    is YamiboResult.Success -> refreshed.value
                    else -> optimisticRepairedPageInfo(pageInfo, repairOption.value)
                }
                seenRepairValues = mutableSetOf()
            }
        }

        val message = when {
            repairCount > 0 -> msg("sign.repair_completed", lastMessage, repairCount)
            else -> lastMessage
        }
        updateTodayRecord(pageInfo, message)
        return YamiboResult.Success(
            SignRepository.ActionResult(
                status = finalStatus,
                message = message,
                repairCount = repairCount,
                pageInfo = pageInfo,
            )
        )
    }

    override suspend fun getTodayRecord(): SignRepository.DailyRecord? {
        return recordQueries.getByDateKey(currentLocalDateKey()).executeAsOneOrNull()?.toModel()
    }

    override suspend fun isSignedToday(): Boolean {
        getCachedPageInfo()?.let { cached ->
            if (cached.hasSignedToday) return true
        }
        return getTodayRecord()?.isSigned == true
    }

    override suspend fun markTodaySigned(message: String?) {
        val info = getCachedPageInfo()?.let(::optimisticSignedPageInfo)
            ?: SignRepository.SignPageInfo(
                currentDateText = null,
                monthLabel = null,
                notice = null,
                calendarDays = emptyList(),
                repairOptions = emptyList(),
                myActivity = emptyList(),
                statistics = emptyList(),
                extraSections = emptyList(),
                signActionUrl = null,
                repairActionPrefix = null,
                hasSignedToday = true,
                lastSignDateKey = currentLocalDateKey(),
            )
        updateTodayRecord(info, message)
    }

    override fun getCachedPageInfo(): SignRepository.SignPageInfo? {
        val cacheUpdatedAt = appSettingsRepository.signPageHtmlCacheUpdatedAt.getValue().toLongOrNull()
            ?: return null
        if (currentLocalDateKeyAt(cacheUpdatedAt) != currentLocalDateKey()) {
            return null
        }
        val html = appSettingsRepository.signPageHtmlCache.getValue().trim()
        return parseSignPageFromHtml(html)
    }

    override fun cacheObservedHtml(html: String): SignRepository.SignPageInfo? {
        val info = parseSignPageFromHtml(html) ?: return null
        appSettingsRepository.signPageHtmlCache.setValue(html)
        appSettingsRepository.signPageHtmlCacheUpdatedAt.setValue(currentTimeMillis().toString())
        updateTodayRecord(info)
        return info
    }

    private fun msg(key: String, vararg args: Any?): String = AppMessage.of(key, *args)

    private suspend fun executeAction(url: String): YamiboResult<ParsedActionResult> {
        val absoluteUrl = buildAbsoluteUrl(url)
        val cookie = authRepository.cookieStore.load().orEmpty()

        return when (val result = yamiboClient.fetchSignAction(absoluteUrl, cookie)) {
            is YamiboResult.Success -> YamiboResult.Success(result.value.toAppModel())
            is YamiboResult.Failure ->
                YamiboResult.Failure(toFriendlyFailure(result.reason), result.exception)

            is YamiboResult.NotLoggedIn -> result
            is YamiboResult.NoPermission -> result
            is YamiboResult.Maintenance -> result
        }
    }

    private fun parseSignPageFromHtml(html: String): SignRepository.SignPageInfo? {
        if (html.isBlank() || !isSignPageLikeHtml(html)) return null
        return when (val result = runBlocking { signPageParser.parse(html) }) {
            is ParseResult.Success -> result.value.toAppModel()
            is ParseResult.NotLoggedIn,
            is ParseResult.NoPermission,
            is ParseResult.Maintenance,
            is ParseResult.Failure -> null
        }
    }

    private fun isSignPageLikeHtml(html: String): Boolean {
        val body = html.lowercase()
        return body.contains("zqlj_sign") ||
            body.contains("repairday") ||
            body.contains("signbtn") ||
            body.contains("tablebody") ||
            html.contains("\u7b7e\u5230") ||
            html.contains("\u7c3d\u5230") ||
            html.contains("\u6253\u5361")
    }

    private fun toFriendlyFailure(reason: String): String {
        val normalized = reason.lowercase()
        return if (
            normalized.contains("cloudflare") ||
            normalized.contains("cf-chl") ||
            normalized.contains("just a moment") ||
            normalized.contains("verify you are human")
        ) {
            msg("sign.cloudflare_required")
        } else {
            reason
        }
    }

    private fun buildAbsoluteUrl(url: String): String {
        return YamiboRoute.Domain.toFullLink(url)
    }

    private fun SignPage.toAppModel(): SignRepository.SignPageInfo {
        return SignRepository.SignPageInfo(
            currentDateText = currentDateText,
            monthLabel = monthLabel,
            notice = notice,
            calendarDays = calendarDays.map {
                SignRepository.CalendarDay(
                    day = it.day,
                    isSigned = it.isSigned,
                    isToday = it.isToday,
                )
            },
            repairOptions = repairOptions.map {
                SignRepository.RepairOption(
                    value = it.value,
                    label = it.label,
                )
            },
            myActivity = myActivity,
            statistics = statistics,
            extraSections = extraSections.map {
                SignRepository.InfoSection(
                    title = it.title,
                    items = it.items,
                )
            },
            signActionUrl = signActionUrl,
            repairActionPrefix = repairActionPrefix,
            hasSignedToday = hasSignedToday,
            lastSignDateKey = lastSignDateKey,
        )
    }

    private fun SignActionResult.toAppModel(): ParsedActionResult {
        val status = when (status) {
            SignActionStatus.Success -> SignRepository.ActionStatus.SUCCESS
            SignActionStatus.AlreadySigned -> SignRepository.ActionStatus.ALREADY_SIGNED
            SignActionStatus.RepairSuccess -> SignRepository.ActionStatus.REPAIR_SUCCESS
        }
        return ParsedActionResult(
            status = status,
            message = message.ifBlank { msg("sign.action_completed") },
        )
    }

    private fun optimisticSignedPageInfo(info: SignRepository.SignPageInfo): SignRepository.SignPageInfo {
        return info.copy(
            signActionUrl = null,
            hasSignedToday = true,
        )
    }

    private fun optimisticRepairedPageInfo(
        info: SignRepository.SignPageInfo,
        repairedValue: String,
    ): SignRepository.SignPageInfo {
        return optimisticSignedPageInfo(info).copy(
            repairOptions = info.repairOptions.filterNot { it.value == repairedValue }
        )
    }

    private fun SignRepository.ActionStatus.isTodaySignedStatus(): Boolean {
        return this == SignRepository.ActionStatus.SUCCESS ||
            this == SignRepository.ActionStatus.ALREADY_SIGNED ||
            this == SignRepository.ActionStatus.REPAIR_SUCCESS
    }

    private fun updateTodayRecord(
        info: SignRepository.SignPageInfo,
        message: String? = null,
    ) {
        val dateKey = currentLocalDateKey()
        recordQueries.upsert(
            dateKey = dateKey,
            isSigned = if (info.hasSignedToday) 1L else 0L,
            updatedAt = currentTimeMillis(),
            lastMessage = message,
        )
    }

    private fun SignDailyRecord.toModel(): SignRepository.DailyRecord {
        return SignRepository.DailyRecord(
            dateKey = dateKey,
            isSigned = isSigned != 0L,
            updatedAt = updatedAt,
            lastMessage = lastMessage,
        )
    }

    private data class ParsedActionResult(
        val status: SignRepository.ActionStatus,
        val message: String,
    )
}

