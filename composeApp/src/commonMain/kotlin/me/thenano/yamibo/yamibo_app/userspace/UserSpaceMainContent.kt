package me.thenano.yamibo.yamibo_app.userspace

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.littlesurvival.dto.model.BlogSummary
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.model.User
import me.thenano.yamibo.yamibo_app.components.YamiboEmptyContent
import me.thenano.yamibo.yamibo_app.components.YamiboPageNavigation
import me.thenano.yamibo.yamibo_app.forum.components.ThreadCard
import me.thenano.yamibo.yamibo_app.userspace.blog.components.BlogCard
import me.thenano.yamibo.yamibo_app.userspace.components.FriendCard
import me.thenano.yamibo.yamibo_app.userspace.components.ReplyGroupCard
import me.thenano.yamibo.yamibo_app.userspace.notification.NoticeCard
import me.thenano.yamibo.yamibo_app.userspace.notification.PrivateMessageCard

@Composable
internal fun UserSpaceMainContent(
    content: UserSpaceContent,
    isSelf: Boolean,
    selectedTab: UserSpaceTab,
    currentPage: Int,
    onNavigateGroup: (UserSpaceGroup, UserSpaceTab) -> Unit,
    onPageChange: (Int) -> Unit,
    onThreadClick: (ThreadSummary) -> Unit,
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
                if (content.page.threads.isEmpty()) {
                    item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                }
                items(content.page.threads, key = { it.tid.value }) { thread ->
                    ThreadCard(
                        thread = thread,
                        onClick = { onThreadClick(thread) },
                        onAuthorClick = { user -> onUserClick(user) },
                    )
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Replies -> {
                if (content.page.replies.isEmpty()) {
                    item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                }
                items(content.page.replies, key = { it.tId.value }) { reply ->
                    ReplyGroupCard(reply, onThreadClick = { onThreadClick(it) }, onQuoteClick = onReplyQuoteClick)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Blogs -> {
                if (content.page.blogs.isEmpty()) {
                    item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                }
                items(content.page.blogs, key = { it.bId.value }) { blog ->
                    BlogCard(blog, onClick = { onBlogClick(blog) }, onUserClick = onUserClick)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Friends -> {
                if (content.page.users.isEmpty()) {
                    item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                }
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
            is UserSpaceContent.Messages -> {
                if (content.page.messages.isEmpty()) {
                    item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                }
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
                if (content.page.notices.isEmpty()) {
                    item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                }
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
    UserSpaceTab.Messages -> "沒有找到消息"
    UserSpaceTab.Notices -> "沒有找到提醒"
}

@Composable
private fun UserSpacePageNavigation(pageNav: PageNav, currentPage: Int, onPageChange: (Int) -> Unit) {
    YamiboPageNavigation(pageNav = pageNav, currentPage = currentPage, onPageChange = onPageChange)
}
