package me.thenano.yamibo.yamibo_app.userspace

import YamiboIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.page.*
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalUserSpaceRepository
import me.thenano.yamibo.yamibo_app.components.*
import me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.userspace.blog.IBlogReaderScreen
import me.thenano.yamibo.yamibo_app.userspace.notification.IPrivateMessageScreen
import me.thenano.yamibo.yamibo_app.webview.action.IActionWebView

enum class UserSpaceTab(val selfTitle: String, val otherTitle: String = selfTitle) {
    Profile("我的資料", "Ta的資料"),
    Threads("我的主題", "Ta的主題"),
    Replies("我的回覆", "Ta的回覆"),
    MyBlogs("我的日志", "Ta的日志"),
    FriendBlogs("好友的日志"),
    ViewAllBlogs("隨便看看"),
    Friends("我的好友"),
    Online("在線成員"),
    Visitors("我的訪客"),
    Traces("我的足跡"),
    Messages("我的消息"),
    Notices("我的提醒");

    fun title(isSelf: Boolean): String = if (isSelf) selfTitle else otherTitle
}

enum class UserSpaceGroup {
    Space,
    Threads,
    Blogs,
    Friends,
    Messages,
}

private sealed interface UserSpaceState {
    data object Loading : UserSpaceState
    data class Success(val content: UserSpaceContent) : UserSpaceState
    data class Error(val message: String) : UserSpaceState
}

internal sealed interface UserSpaceContent {
    data class Profile(val page: ProfilePage) : UserSpaceContent
    data class Threads(val page: UserSpaceThreadPage) : UserSpaceContent
    data class Replies(val page: UserSpaceThreadReplyPage) : UserSpaceContent
    data class Blogs(val page: UserSpaceBlogPage) : UserSpaceContent
    data class Friends(val page: UserSpaceFriendPage) : UserSpaceContent
    data class Messages(val page: UserSpacePrivateMessagePage) : UserSpaceContent
    data class Notices(val page: UserSpaceNoticePage) : UserSpaceContent
}

private enum class ViewAllBlogFilter(
    val title: String,
    val apiType: YamiboRoute.UserSpace.Blog.ViewAllType,
) {
    Latest("最新發表的日志", YamiboRoute.UserSpace.Blog.ViewAllType.Latest),
    Hot("推薦閱讀的日志", YamiboRoute.UserSpace.Blog.ViewAllType.Hot),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSpacePage(
    userId: UserId? = null,
    titleHint: String? = null,
    group: UserSpaceGroup = UserSpaceGroup.Space,
    initialTab: UserSpaceTab = UserSpaceTab.Profile,
    mainTabTopBar: Boolean = false,
) {
    val colors = YamiboTheme.colors
    val repository = LocalUserSpaceRepository.current
    val authRepository = LocalAuthRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser = authRepository.currentUser()
    val isSelf = userId == null || currentUser?.uid?.value == userId.value
    val tabs = remember(isSelf, group) { tabsFor(group, isSelf) }

    var selectedTab by remember(userId, group, initialTab) {
        mutableStateOf(if (initialTab in tabs) initialTab else tabs.first())
    }
    var currentPage by remember { mutableIntStateOf(1) }
    var state by remember { mutableStateOf<UserSpaceState>(UserSpaceState.Loading) }
    var profile by remember { mutableStateOf(repository.getCachedProfile(userId)) }
    var isRefreshing by remember { mutableStateOf(false) }
    var viewAllBlogFilter by remember(userId, group) { mutableStateOf(ViewAllBlogFilter.Latest) }

    suspend fun loadTab(tab: UserSpaceTab, page: Int, preferCache: Boolean = true) {
        if (preferCache) {
            cachedContent(repository, userId, tab, page, viewAllBlogFilter.apiType)?.let {
                currentPage = page
                state = UserSpaceState.Success(it)
                return
            }
        }

        val result = fetchContent(repository, userId, tab, page, viewAllBlogFilter.apiType)
        state = when (result) {
            is YamiboResult.Success -> {
                currentPage = result.value.pageNumber() ?: page
                UserSpaceState.Success(result.value)
            }
            else -> UserSpaceState.Error(result.message())
        }
    }

    LaunchedEffect(userId) {
        profile = repository.getCachedProfile(userId)
        if (profile == null) {
            when (val result = repository.fetchProfile(userId)) {
                is YamiboResult.Success -> profile = result.value
                else -> Unit
            }
        }
    }

    LaunchedEffect(userId, group, selectedTab, viewAllBlogFilter) {
        currentPage = 1
        state = UserSpaceState.Loading
        loadTab(selectedTab, 1)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.creamBackground,
        snackbarHost = { YamiboSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (mainTabTopBar) {
                UserSpaceMainTopBar(
                    title = group.mainTitle(),
                    profile = currentUser,
                    onSpaceClick = {
                        navigator.navigate(IUserSpaceScreen(currentUser?.uid, currentUser?.username))
                    },
                )
            } else {
                UserSpaceTopBar(
                    title = topBarTitle(profile, titleHint, group, selectedTab, isSelf),
                    showEdit = isSelf && (
                        group == UserSpaceGroup.Blogs ||
                            (group == UserSpaceGroup.Messages && selectedTab == UserSpaceTab.Messages)
                    ),
                    applyStatusPadding = true,
                    onBack = { navigator.pop() },
                    onEdit = {
                        navigator.navigate(userSpaceEditActionWebView(group))
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(colors.creamBackground)
        ) {
            if (tabs.size > 1) {
                UserSpaceTabRow(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    isSelf = isSelf,
                    onSelect = { selectedTab = it }
                )
            }
            if (selectedTab == UserSpaceTab.ViewAllBlogs) {
                UserSpaceViewAllBlogFilterRow(
                    selected = viewAllBlogFilter,
                    onSelect = { viewAllBlogFilter = it },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "userspace_state",
                ) { current ->
                    when (current) {
                        UserSpaceState.Loading -> UserSpaceLoadingContent()
                        is UserSpaceState.Error -> UserSpaceErrorContent(
                            message = current.message,
                            onRetry = {
                                state = UserSpaceState.Loading
                                scope.launch { loadTab(selectedTab, currentPage, preferCache = false) }
                            }
                        )
                        is UserSpaceState.Success -> PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                scope.launch {
                                    loadTab(selectedTab, currentPage, preferCache = false)
                                    if (selectedTab == UserSpaceTab.Profile) {
                                        when (val result = repository.fetchProfile(userId)) {
                                            is YamiboResult.Success -> profile = result.value
                                            else -> Unit
                                        }
                                    }
                                    isRefreshing = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            UserSpaceMainContent(
                                content = current.content,
                                isSelf = isSelf,
                                selectedTab = selectedTab,
                                currentPage = currentPage,
                                onNavigateGroup = { targetGroup, targetTab ->
                                    navigator.navigate(
                                        IUserSpaceScreen(
                                            userId = userId,
                                            titleHint = profile?.username ?: titleHint,
                                            group = targetGroup,
                                            initialTab = targetTab,
                                        )
                                    )
                                },
                                onPageChange = { page ->
                                    state = UserSpaceState.Loading
                                    scope.launch { loadTab(selectedTab, page) }
                                },
                                onThreadClick = { thread -> navigateThread(thread, navigator) },
                                onUserClick = { user -> navigator.navigate(IUserSpaceScreen(user.uid, user.name)) },
                                onOpenPrivateMessage = { user -> navigator.navigate(IPrivateMessageScreen(user.uid, user.name)) },
                                onBlogClick = { blog ->
                                    navigator.navigate(
                                        IBlogReaderScreen(
                                            blogId = blog.bId,
                                            userId = blog.author.uid,
                                            titleHint = blog.title,
                                        )
                                    )
                                },
                                onReplyQuoteClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("TODO: 回覆定位跳轉尚未接入", duration = SnackbarDuration.Short)
                                    }
                                },
                                onMessageAction = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("TODO: 消息互動尚未接入", duration = SnackbarDuration.Short)
                                    }
                                },
                                onOpenWebView = { title, url ->
                                    navigator.navigate(
                                        IActionWebView(
                                            title = title,
                                            initialUrl = fullYamiboUrl(url),
                                            successCondition = { false },
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun tabsFor(group: UserSpaceGroup, isSelf: Boolean): List<UserSpaceTab> = when (group) {
    UserSpaceGroup.Space -> listOf(UserSpaceTab.Profile)
    UserSpaceGroup.Threads -> listOf(UserSpaceTab.Threads, UserSpaceTab.Replies)
    UserSpaceGroup.Blogs -> if (isSelf) {
        listOf(UserSpaceTab.FriendBlogs, UserSpaceTab.MyBlogs, UserSpaceTab.ViewAllBlogs)
    } else {
        listOf(UserSpaceTab.MyBlogs)
    }
    UserSpaceGroup.Friends -> listOf(
        UserSpaceTab.Friends,
        UserSpaceTab.Online,
        UserSpaceTab.Visitors,
        UserSpaceTab.Traces,
    )
    UserSpaceGroup.Messages -> listOf(UserSpaceTab.Messages, UserSpaceTab.Notices)
}

private fun UserSpaceGroup.mainTitle(): String = when (this) {
    UserSpaceGroup.Space -> "我的資料"
    UserSpaceGroup.Threads -> "我的主題"
    UserSpaceGroup.Blogs -> "我的日志"
    UserSpaceGroup.Friends -> "我的好友"
    UserSpaceGroup.Messages -> "我的消息"
}

@Composable
private fun UserSpaceViewAllBlogFilterRow(
    selected: ViewAllBlogFilter,
    onSelect: (ViewAllBlogFilter) -> Unit,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.creamSurface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ViewAllBlogFilter.entries.forEach { filter ->
            YamiboActionChip(text = filter.title, selected = filter == selected, onClick = { onSelect(filter) })
        }
    }
    HorizontalDivider(color = colors.brownLight.copy(alpha = 0.45f))
}

@Composable
private fun UserSpaceTopBar(
    title: String,
    showEdit: Boolean,
    applyStatusPadding: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    YamiboTopBar(
        title = title,
        applyStatusPadding = applyStatusPadding,
        onBack = onBack,
    ) {
        if (showEdit) {
            YamiboTopBarIconAction(YamiboIcons.EditOrSign, "編輯", onEdit)
        }
    }
}

@Composable
private fun UserSpaceMainTopBar(
    title: String,
    profile: ProfilePage?,
    onSpaceClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    YamiboMainTabTopBar(
        title = title,
    ) {
        Surface(onClick = onSpaceClick, shape = RoundedCornerShape(18.dp), color = Color.Transparent) {
            Row(
                modifier = Modifier.padding(start = 6.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                UserAvatar(profile?.avatarUrl, size = 28)
                Text(
                    text = "我的空間",
                    color = colors.brownDeep,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun UserSpaceTabRow(
    tabs: List<UserSpaceTab>,
    selectedTab: UserSpaceTab,
    isSelf: Boolean,
    onSelect: (UserSpaceTab) -> Unit,
) {
    val colors = YamiboTheme.colors
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    ScrollableYamiboTabRow(
        selectedIndex = selectedIndex,
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        text = tab.title(isSelf),
                        color = colors.brownDeep,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
            )
        }
    }
}

@Composable
private fun ScrollableYamiboTabRow(
    selectedIndex: Int,
    content: @Composable () -> Unit,
) {
    val colors = YamiboTheme.colors
    @Suppress("DEPRECATION")
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = colors.creamSurface,
        contentColor = colors.brownDeep,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = colors.brownDeep,
                    height = 2.dp,
                )
            }
        },
        divider = {
            HorizontalDivider(color = colors.brownLight.copy(alpha = 0.45f))
        },
    ) {
        content()
    }
}

@Composable
private fun UserSpaceLoadingContent() {
    YamiboLoadingContent()
}

@Composable
private fun UserSpaceErrorContent(message: String, onRetry: () -> Unit) {
    YamiboErrorContent(message = message, onRetry = onRetry)
}

private suspend fun fetchContent(
    repository: me.thenano.yamibo.yamibo_app.repository.UserSpaceRepository,
    userId: UserId?,
    tab: UserSpaceTab,
    page: Int,
    viewAllType: YamiboRoute.UserSpace.Blog.ViewAllType,
): YamiboResult<UserSpaceContent> {
    return when (tab) {
        UserSpaceTab.Profile -> repository.fetchProfile(userId).mapSuccess { UserSpaceContent.Profile(it) }
        UserSpaceTab.Threads -> repository.fetchThreads(userId, page).mapSuccess { UserSpaceContent.Threads(it) }
        UserSpaceTab.Replies -> repository.fetchReplies(userId, page).mapSuccess { UserSpaceContent.Replies(it) }
        UserSpaceTab.MyBlogs -> repository.fetchMyBlogs(userId, page).mapSuccess { UserSpaceContent.Blogs(it) }
        UserSpaceTab.FriendBlogs -> repository.fetchFriendBlogs(page).mapSuccess { UserSpaceContent.Blogs(it) }
        UserSpaceTab.ViewAllBlogs -> repository.fetchViewAllBlogs(viewAllType, page).mapSuccess { UserSpaceContent.Blogs(it) }
        UserSpaceTab.Friends -> repository.fetchFriends(YamiboRoute.UserSpace.FriendPageType.MyFriend, page).mapSuccess { UserSpaceContent.Friends(it) }
        UserSpaceTab.Online -> repository.fetchFriends(YamiboRoute.UserSpace.FriendPageType.OnlineMember, page).mapSuccess { UserSpaceContent.Friends(it) }
        UserSpaceTab.Visitors -> repository.fetchFriends(YamiboRoute.UserSpace.FriendPageType.MyVisitor, page).mapSuccess { UserSpaceContent.Friends(it) }
        UserSpaceTab.Traces -> repository.fetchFriends(YamiboRoute.UserSpace.FriendPageType.MyTrace, page).mapSuccess { UserSpaceContent.Friends(it) }
        UserSpaceTab.Messages -> repository.fetchPrivateMessages(page).mapSuccess { UserSpaceContent.Messages(it) }
        UserSpaceTab.Notices -> repository.fetchNotices(page).mapSuccess { UserSpaceContent.Notices(it) }
    }
}

private fun cachedContent(
    repository: me.thenano.yamibo.yamibo_app.repository.UserSpaceRepository,
    userId: UserId?,
    tab: UserSpaceTab,
    page: Int,
    viewAllType: YamiboRoute.UserSpace.Blog.ViewAllType,
): UserSpaceContent? {
    return when (tab) {
        UserSpaceTab.Profile -> repository.getCachedProfile(userId)?.let { UserSpaceContent.Profile(it) }
        UserSpaceTab.Threads -> repository.getCachedThreads(userId, page)?.let { UserSpaceContent.Threads(it) }
        UserSpaceTab.Replies -> repository.getCachedReplies(userId, page)?.let { UserSpaceContent.Replies(it) }
        UserSpaceTab.MyBlogs -> repository.getCachedMyBlogs(userId, page)?.let { UserSpaceContent.Blogs(it) }
        UserSpaceTab.FriendBlogs -> repository.getCachedFriendBlogs(page)?.let { UserSpaceContent.Blogs(it) }
        UserSpaceTab.ViewAllBlogs -> repository.getCachedViewAllBlogs(viewAllType, page)?.let { UserSpaceContent.Blogs(it) }
        UserSpaceTab.Friends -> repository.getCachedFriends(YamiboRoute.UserSpace.FriendPageType.MyFriend, page)?.let { UserSpaceContent.Friends(it) }
        UserSpaceTab.Online -> repository.getCachedFriends(YamiboRoute.UserSpace.FriendPageType.OnlineMember, page)?.let { UserSpaceContent.Friends(it) }
        UserSpaceTab.Visitors -> repository.getCachedFriends(YamiboRoute.UserSpace.FriendPageType.MyVisitor, page)?.let { UserSpaceContent.Friends(it) }
        UserSpaceTab.Traces -> repository.getCachedFriends(YamiboRoute.UserSpace.FriendPageType.MyTrace, page)?.let { UserSpaceContent.Friends(it) }
        UserSpaceTab.Messages -> repository.getCachedPrivateMessages(page)?.let { UserSpaceContent.Messages(it) }
        UserSpaceTab.Notices -> repository.getCachedNotices(page)?.let { UserSpaceContent.Notices(it) }
    }
}

private fun UserSpaceContent.pageNumber(): Int? = when (this) {
    is UserSpaceContent.Profile -> null
    is UserSpaceContent.Threads -> page.pageNav?.currentPage
    is UserSpaceContent.Replies -> page.pageNav?.currentPage
    is UserSpaceContent.Blogs -> page.pageNav?.currentPage
    is UserSpaceContent.Friends -> page.pageNav?.currentPage
    is UserSpaceContent.Messages -> page.pageNav?.currentPage
    is UserSpaceContent.Notices -> page.pageNav?.currentPage
}

private fun <T, R> YamiboResult<T>.mapSuccess(transform: (T) -> R): YamiboResult<R> = when (this) {
    is YamiboResult.Success -> YamiboResult.Success(transform(value))
    is YamiboResult.Failure -> this
    is YamiboResult.NotLoggedIn -> this
    is YamiboResult.NoPermission -> this
    is YamiboResult.Maintenance -> this
}

private fun topBarTitle(
    profile: ProfilePage?,
    titleHint: String?,
    group: UserSpaceGroup,
    tab: UserSpaceTab,
    isSelf: Boolean,
): String {
    val name = profile?.username ?: titleHint ?: "用戶"
    return when (group) {
        UserSpaceGroup.Space -> if (isSelf) "我的資料" else "${name}的資料"
        UserSpaceGroup.Threads -> if (isSelf) "我的主題" else "$name - Ta的主題"
        UserSpaceGroup.Blogs -> if (isSelf) "我的日志" else "$name - Ta的日志"
        UserSpaceGroup.Friends -> tab.title(isSelf)
        UserSpaceGroup.Messages -> tab.title(isSelf)
    }
}

private fun navigateThread(thread: ThreadSummary, navigator: ComposableNavigator) {
    val fid = thread.fid
    if (fid != null && YamiboForum.isNovelForum(fid)) {
        navigator.navigate(INovelThreadDetailScreen(thread.tid, thread.title, thread.author?.uid))
    } else {
        navigator.navigate(IThreadReaderScreen(tid = thread.tid, title = thread.title))
    }
}

private fun fullYamiboUrl(url: String): String =
    YamiboRoute.Domain.toFullLink(url)

private fun userSpaceEditActionWebView(group: UserSpaceGroup): IActionWebView {
    return when (group) {
        UserSpaceGroup.Blogs -> IActionWebView(
            title = "發日志",
            initialUrl = YamiboRoute.SendBlogPage.build(),
            successCondition = { url -> isSendBlogSuccessUrl(url) },
        )
        else -> IActionWebView(
            title = "發消息",
            initialUrl = YamiboRoute.SendPrivateMessagePage.build(),
            successCondition = { url -> isMessageListUrl(url) },
        )
    }
}

private fun isMessageListUrl(url: String): Boolean {
    val target = YamiboRoute.UserSpace.Notification(
        type = YamiboRoute.UserSpace.NotificationType.MyMessage,
        page = 1,
    ).build()
    return url.startsWith(target.substringBefore("&page=")) &&
        url.contains("mod=space") &&
        url.contains("do=pm") &&
        !url.contains("spacecp")
}

private fun isSendBlogSuccessUrl(url: String): Boolean {
    return url.startsWith("https://bbs.yamibo.com/home.php?mod=spacecp&ac=blog&blogid=")
}
