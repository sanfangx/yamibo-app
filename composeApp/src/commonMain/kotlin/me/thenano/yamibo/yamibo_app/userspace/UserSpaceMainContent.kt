package me.thenano.yamibo.yamibo_app.userspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import io.github.littlesurvival.dto.model.BlogSummary
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.model.User
import me.thenano.yamibo.yamibo_app.components.YamiboActionChip
import me.thenano.yamibo.yamibo_app.components.YamiboEmptyContent
import me.thenano.yamibo.yamibo_app.components.YamiboPageNavigation
import me.thenano.yamibo.yamibo_app.favorite.updates.FavoriteUpdateStatusCard
import me.thenano.yamibo.yamibo_app.forum.components.ThreadCard
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.userspace.blog.components.BlogCard
import me.thenano.yamibo.yamibo_app.userspace.components.FriendCard
import me.thenano.yamibo.yamibo_app.userspace.components.ReplyGroupCard
import me.thenano.yamibo.yamibo_app.userspace.notification.NoticeCard
import me.thenano.yamibo.yamibo_app.userspace.notification.PrivateMessageCard
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

@Composable
internal fun UserSpaceMainContent(
    content: UserSpaceContent,
    isSelf: Boolean,
    selectedTab: UserSpaceTab,
    currentPage: Int,
    onNavigateGroup: (UserSpaceGroup, UserSpaceTab) -> Unit,
    onPageChange: (Int) -> Unit,
    onThreadClick: (ThreadSummary) -> Unit,
    onUpdateEventClick: (FavoriteUpdateRepository.UpdateEvent) -> Unit,
    onDismissUpdateEvent: (FavoriteUpdateRepository.UpdateEvent) -> Unit,
    onStartFavoriteUpdate: () -> Unit,
    onGlobalFavoriteUpdate: () -> Unit,
    onCancelFavoriteUpdate: (String) -> Unit,
    onToggleFavoriteUpdateFid: (Int, Boolean) -> Unit,
    onUserClick: (User) -> Unit,
    onOpenPrivateMessage: (User) -> Unit,
    onBlogClick: (BlogSummary) -> Unit,
    onReplyQuoteClick: () -> Unit,
    onMessageAction: () -> Unit,
    onOpenWebView: (String, String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        when (content) {
            is UserSpaceContent.Profile -> {
                item {
                    UserSpaceProfileHeader(
                        profile = content.page,
                        isSelf = isSelf,
                        onNavigateGroup = onNavigateGroup,
                        onOpenWebView = onOpenWebView,
                    )
                }
            }
            is UserSpaceContent.Threads -> {
                if (content.page.threads.isEmpty()) item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                items(content.page.threads, key = { it.tid.value }) { thread ->
                    ThreadCard(thread = thread, onClick = { onThreadClick(thread) }, onAuthorClick = onUserClick)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Replies -> {
                if (content.page.replies.isEmpty()) item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                items(content.page.replies, key = { it.tId.value }) { reply ->
                    ReplyGroupCard(reply, onThreadClick = { onThreadClick(it) }, onQuoteClick = onReplyQuoteClick)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Blogs -> {
                if (content.page.blogs.isEmpty()) item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                items(content.page.blogs, key = { it.bId.value }) { blog ->
                    BlogCard(blog, onClick = { onBlogClick(blog) }, onUserClick = onUserClick)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Friends -> {
                if (content.page.users.isEmpty()) item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                items(content.page.users, key = { it.user.uid.value }) { item ->
                    FriendCard(
                        item = item,
                        onUserClick = { onUserClick(item.user) },
                        onMessageClick = item.pmUrl?.let { { onOpenPrivateMessage(item.user) } },
                        onDeleteClick = onMessageAction,
                    )
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Updates -> {
                item {
                    FavoriteUpdateHeader(
                        filters = content.filters,
                        runState = content.runState,
                        onStartFavoriteUpdate = onStartFavoriteUpdate,
                        onGlobalFavoriteUpdate = onGlobalFavoriteUpdate,
                        onCancelFavoriteUpdate = onCancelFavoriteUpdate,
                        onToggleFavoriteUpdateFid = onToggleFavoriteUpdateFid,
                    )
                }
                if (content.events.isEmpty()) item { UserSpaceEmptyListMessage("沒有偵測到更新") }
                items(content.events, key = { it.id }) { event ->
                    FavoriteUpdateCard(event = event, onClick = { onUpdateEventClick(event) })
                }
            }
            is UserSpaceContent.Messages -> {
                if (content.page.messages.isEmpty()) item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                items(content.page.messages, key = { "${it.user.uid.value}_${it.timeInfo.text}" }) { message ->
                    PrivateMessageCard(
                        message,
                        onUserClick = { onUserClick(message.user) },
                        onAction = { onOpenPrivateMessage(message.user) },
                    )
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Notices -> {
                if (content.page.notices.isEmpty()) item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                items(content.page.notices, key = { it.noticeId.value }) { notice ->
                    NoticeCard(notice, onAction = onMessageAction)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
        }
    }
}

@Composable
private fun UserSpaceEmptyListMessage(message: String) {
    YamiboEmptyContent(message = message, modifier = Modifier.padding(horizontal = 24.dp, vertical = 80.dp))
}

@Composable
private fun FavoriteUpdateHeader(
    filters: List<FavoriteUpdateRepository.FidFilter>,
    runState: FavoriteUpdateRepository.RunState,
    onStartFavoriteUpdate: () -> Unit,
    onGlobalFavoriteUpdate: () -> Unit,
    onCancelFavoriteUpdate: (String) -> Unit,
    onToggleFavoriteUpdateFid: (Int, Boolean) -> Unit,
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    val running = (runState as? FavoriteUpdateRepository.RunState.Running)?.snapshot

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("收藏更新", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                YamiboActionChip("篩選", onClick = { showFilterDialog = true })
                YamiboActionChip(
                    text = if (running != null) "取消" else "全域刷新",
                    onClick = {
                        if (running != null) onCancelFavoriteUpdate(running.runId) else onGlobalFavoriteUpdate()
                    },
                )
            }
        }

        if (runState !is FavoriteUpdateRepository.RunState.Idle) {
            FavoriteUpdateStatusCard(
                state = runState,
                modifier = Modifier.fillMaxWidth(),
                onCancel = onCancelFavoriteUpdate,
            )
        }
    }

    if (showFilterDialog) {
        FavoriteUpdateFidFilterDialog(
            filters = filters,
            onDismiss = { showFilterDialog = false },
            onToggle = onToggleFavoriteUpdateFid,
        )
    }
}

@Composable
private fun FavoriteUpdateFidFilterDialog(
    filters: List<FavoriteUpdateRepository.FidFilter>,
    onDismiss: () -> Unit,
    onToggle: (Int, Boolean) -> Unit,
) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("篩選類別", color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.height(360.dp)) {
                items(filters, key = { it.fid }) { filter ->
                    Surface(
                        onClick = { onToggle(filter.fid, !filter.enabled) },
                        color = if (filter.enabled) colors.brownPrimary.copy(alpha = 0.16f) else colors.creamSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = filter.enabled,
                                onClick = { onToggle(filter.fid, !filter.enabled) },
                                colors = RadioButtonDefaults.colors(selectedColor = colors.brownDeep),
                            )
                            Text(
                                text = "${filter.forumName} (${filter.itemCount})",
                                color = colors.textDark,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { YamiboActionChip("關閉", onDismiss) },
        containerColor = colors.creamSurface,
    )
}

@Composable
private fun FavoriteUpdateCard(
    event: FavoriteUpdateRepository.UpdateEvent,
    onClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .width(82.dp)
                    .aspectRatio(0.72f),
                colors = CardDefaults.cardColors(containerColor = colors.brownLight.copy(alpha = 0.2f)),
            ) {
                val coverUrl = event.coverUrl
                if (!coverUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = rememberImageRequest(url = coverUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = event.title,
                            color = colors.brownDeep.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = event.title,
                    color = colors.textDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = event.latestPostTitle?.takeIf { it.isNotBlank() } ?: event.summary,
                    color = colors.textDark.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                event.forumName?.takeIf { it.isNotBlank() }?.let {
                    Text("#$it", color = colors.textDark.copy(alpha = 0.56f), fontSize = 12.sp)
                }
                Text(
                    text = "${event.summary} / ${formatUpdateRelativeTime(event.detectedAt)}",
                    color = colors.textDark.copy(alpha = 0.48f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatUpdateRelativeTime(timestamp: Long): String {
    val elapsed = (currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = elapsed / 1000L / 60L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        days > 0L -> "${days}天前"
        hours > 0L -> "${hours}小時前"
        minutes > 0L -> "${minutes}分鐘前"
        else -> "剛剛"
    }
}

private fun emptyMessage(tab: UserSpaceTab, isSelf: Boolean): String = when (tab) {
    UserSpaceTab.Profile -> ""
    UserSpaceTab.Threads -> if (isSelf) "沒有找到主題" else "沒有找到Ta的主題"
    UserSpaceTab.Replies -> if (isSelf) "沒有找到回復" else "沒有找到Ta的回復"
    UserSpaceTab.MyBlogs -> if (isSelf) "沒有找到日志" else "沒有找到Ta的日志"
    UserSpaceTab.FriendBlogs -> "沒有找到好友的日志"
    UserSpaceTab.ViewAllBlogs -> "沒有找到日志"
    UserSpaceTab.Friends -> "沒有找到好友"
    UserSpaceTab.Online -> "沒有找到在線成員"
    UserSpaceTab.Visitors -> "沒有找到訪客"
    UserSpaceTab.Traces -> "沒有找到足跡"
    UserSpaceTab.Updates -> "沒有偵測到更新"
    UserSpaceTab.Messages -> "沒有找到消息"
    UserSpaceTab.Notices -> "沒有找到提醒"
}

@Composable
private fun UserSpacePageNavigation(pageNav: PageNav, currentPage: Int, onPageChange: (Int) -> Unit) {
    YamiboPageNavigation(pageNav = pageNav, currentPage = currentPage, onPageChange = onPageChange)
}
