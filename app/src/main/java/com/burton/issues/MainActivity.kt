package com.burton.issues

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.burton.issues.data.deeplink.NewIssueIntents
import com.burton.issues.data.github.GitHubOAuth
import com.burton.issues.data.repository.IssuesRepository
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.burton.issues.report.ShakeToReport
import com.burton.issues.ui.apps.AppsScreen
import com.burton.issues.ui.composeissue.ComposeIssueScreen
import com.burton.issues.ui.home.HomeScreen
import com.burton.issues.ui.issue.IssueScreen
import com.burton.issues.ui.issues.IssueListScreen
import com.burton.issues.ui.navigation.Routes
import com.burton.issues.ui.search.SearchScreen
import com.burton.issues.ui.signin.SignInScreen
import com.burton.issues.ui.theme.BurtonBlack
import com.burton.issues.ui.theme.BurtonIssuesTheme
import com.burton.issues.ui.theme.BurtonIvory
import com.burton.issues.ui.theme.BurtonMute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var repository: IssuesRepository
    private val shakeToReport by lazy { ShakeToReport(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncoming(intent)
        enableEdgeToEdge()
        setContent {
            BurtonIssuesTheme {
                val appViewModel: AppViewModel = hiltViewModel()
                val snapshot by appViewModel.state.collectAsStateWithLifecycle()
                if (snapshot.tokenPresent) {
                    BurtonApp(pendingCompose = snapshot.pendingNewIssue != null)
                } else {
                    SignInScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shakeToReport.start()
    }

    override fun onPause() {
        shakeToReport.stop()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    private fun handleIncoming(intent: Intent?) {
        if (intent == null) return
        val uri = intent.data
        if (GitHubOAuth.isCallback(uri?.scheme, uri?.host, uri?.path)) {
            val error = uri?.getQueryParameter("error")
            val description = uri?.getQueryParameter("error_description").orEmpty()
            val code = uri?.getQueryParameter("code").orEmpty()
            val state = uri?.getQueryParameter("state").orEmpty()
            lifecycleScope.launch {
                if (!error.isNullOrBlank()) {
                    repository.failOauth(
                        description.ifBlank {
                            if (error == "access_denied") "GitHub login was cancelled." else error
                        },
                    )
                } else {
                    runCatching { repository.completeOAuth(code, state) }
                }
            }
            return
        }
        val request = NewIssueIntents.fromIntent(intent, callerPackage(intent)) ?: return
        repository.queueNewIssue(request)
    }

    @Suppress("DEPRECATION")
    private fun callerPackage(intent: Intent): String {
        val extraReferrer = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_REFERRER)
        val candidates = listOf(
            callingPackage.orEmpty(),
            NewIssueIntents.packageFromReferrer(referrer),
            NewIssueIntents.packageFromReferrer(extraReferrer),
        )
        return candidates.firstOrNull { it.isNotBlank() && it != packageName }.orEmpty()
    }
}

@Composable
private fun BurtonApp(pendingCompose: Boolean) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(Routes.INBOX, Routes.APPS, Routes.SEARCH)
    val selectedTab = if (route in tabs) route else Routes.INBOX
    val hideTabs = route?.startsWith("issue") == true ||
        route?.startsWith("issues") == true ||
        route?.startsWith("compose") == true
    LaunchedEffect(pendingCompose) {
        if (pendingCompose && navController.currentDestination?.route?.startsWith("compose") != true) {
            navController.navigate(Routes.compose()) {
                launchSingleTop = true
            }
        }
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BurtonBlack),
        containerColor = BurtonBlack,
        bottomBar = {
            if (!hideTabs) {
                NavigationBar(
                    containerColor = BurtonBlack,
                    contentColor = BurtonIvory,
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    NavigationBarItem(
                        selected = selectedTab == Routes.INBOX,
                        onClick = { navController.goTab(Routes.INBOX) },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = "Inbox") },
                        label = { Text("Inbox") },
                        colors = navColors(selectedTab == Routes.INBOX),
                    )
                    NavigationBarItem(
                        selected = selectedTab == Routes.APPS,
                        onClick = { navController.goTab(Routes.APPS) },
                        icon = { Icon(Icons.Rounded.Inventory2, contentDescription = "Apps") },
                        label = { Text("Apps") },
                        colors = navColors(selectedTab == Routes.APPS),
                    )
                    NavigationBarItem(
                        selected = selectedTab == Routes.SEARCH,
                        onClick = { navController.goTab(Routes.SEARCH) },
                        icon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                        label = { Text("Search") },
                        colors = navColors(selectedTab == Routes.SEARCH),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.INBOX,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable(Routes.INBOX) {
                HomeScreen(
                    onOpenIssue = { repo, number -> navController.navigate(Routes.issue(repo, number)) },
                    onCompose = { repo -> navController.navigate(Routes.compose(repo)) },
                )
            }
            composable(Routes.APPS) {
                AppsScreen(onOpenApp = { navController.navigate(Routes.issues(it)) })
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onOpenIssue = { repo, number -> navController.navigate(Routes.issue(repo, number)) },
                )
            }
            composable(
                Routes.ISSUES,
                arguments = listOf(navArgument("repo") { type = NavType.StringType }),
            ) {
                val repo = Routes.decode(it.arguments?.getString("repo"))
                IssueListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenIssue = { number -> navController.navigate(Routes.issue(repo, number)) },
                    onCompose = { navController.navigate(Routes.compose(repo)) },
                )
            }
            composable(
                Routes.ISSUE,
                arguments = listOf(
                    navArgument("repo") { type = NavType.StringType },
                    navArgument("number") { type = NavType.IntType },
                ),
            ) {
                IssueScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Routes.COMPOSE,
                arguments = listOf(
                    navArgument("repo") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                ),
            ) {
                ComposeIssueScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { repo, number ->
                        navController.popBackStack()
                        navController.navigate(Routes.issue(repo, number))
                    },
                )
            }
        }
    }
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun navColors(selected: Boolean) = NavigationBarItemDefaults.colors(
    selectedIconColor = BurtonIvory,
    selectedTextColor = BurtonIvory,
    unselectedIconColor = BurtonMute,
    unselectedTextColor = BurtonMute,
    indicatorColor = Color(0xFF222222),
)
