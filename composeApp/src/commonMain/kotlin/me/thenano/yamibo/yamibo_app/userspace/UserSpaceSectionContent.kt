package me.thenano.yamibo.yamibo_app.userspace

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

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
import me.thenano.yamibo.yamibo_app.message.MessageCenterTab
import me.thenano.yamibo.yamibo_app.userspace.blog.components.BlogCard
import me.thenano.yamibo.yamibo_app.userspace.components.FriendCard
import me.thenano.yamibo.yamibo_app.userspace.components.ReplyGroupCard

@Composable
internal fun UserSpaceSectionContent(
    content: UserSpaceScreenContent,
    isSelf: Boolean,
    selectedTab: UserSpaceSubPage,
    currentPage: Int,
    onNavigateSection: (UserSpaceSection, UserSpaceSubPage) -> Unit,
    onOpenMessageCenter: (MessageCenterTab) -> Unit,
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
            is UserSpaceScreenContent.Profile -> {
                item {
                    UserSpaceProfileHeader(
                        profile = content.page,
                        isSelf = isSelf,
                        onNavigateSection = onNavigateSection,
                        onOpenMessageCenter = { onOpenMessageCenter(MessageCenterTab.PrivateMessages) },
                        onOpenWebView = onOpenWebView,
                    )
                }
            }
            is UserSpaceScreenContent.Threads -> {
                if (content.page.threads.isEmpty()) item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                items(content.page.threads, key = { it.tid.value }) { thread ->
                    ThreadCard(thread = thread, onClick = { onThreadClick(thread) }, onAuthorClick = onUserClick)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceScreenContent.Replies -> {
                if (content.page.replies.isEmpty()) item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                items(content.page.replies, key = { it.tId.value }) { reply ->
                    ReplyGroupCard(reply, onThreadClick = { onThreadClick(it) }, onQuoteClick = onReplyQuoteClick)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceScreenContent.Blogs -> {
                if (content.page.blogs.isEmpty()) item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                items(content.page.blogs, key = { it.bId.value }) { blog ->
                    BlogCard(blog, onClick = { onBlogClick(blog) }, onUserClick = onUserClick)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceScreenContent.Friends -> {
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
        }
    }
}

@Composable
private fun UserSpaceEmptyListMessage(message: String) {
    YamiboEmptyContent(message = message, modifier = Modifier.padding(horizontal = 24.dp, vertical = 80.dp))
}

private fun emptyMessage(tab: UserSpaceSubPage, isSelf: Boolean): String = when (tab) {
    UserSpaceSubPage.Profile -> ""
    UserSpaceSubPage.Threads -> if (isSelf) appString(Res.string.auto_c4cf0734d5) else appString(Res.string.auto_dcf657b269)
    UserSpaceSubPage.Replies -> if (isSelf) appString(Res.string.auto_c4f415a3e0) else appString(Res.string.auto_a2d1cf26ce)
    UserSpaceSubPage.MyBlogs -> if (isSelf) appString(Res.string.auto_d1a20b1d07) else appString(Res.string.auto_98810a2754)
    UserSpaceSubPage.FriendBlogs -> appString(Res.string.auto_9f9529d11a)
    UserSpaceSubPage.ViewAllBlogs -> appString(Res.string.auto_d1a20b1d07)
    UserSpaceSubPage.Friends -> appString(Res.string.auto_3409bf400f)
    UserSpaceSubPage.Online -> appString(Res.string.auto_abac6fc9bc)
    UserSpaceSubPage.Visitors -> appString(Res.string.auto_50597076cd)
    UserSpaceSubPage.Traces -> appString(Res.string.auto_adab468428)
}

@Composable
private fun UserSpacePageNavigation(pageNav: PageNav, currentPage: Int, onPageChange: (Int) -> Unit) {
    YamiboPageNavigation(pageNav = pageNav, currentPage = currentPage, onPageChange = onPageChange)
}

