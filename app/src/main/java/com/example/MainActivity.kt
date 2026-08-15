package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.ui.AnalysisScreen
import com.example.ui.TimerScreen
import com.example.ui.DashboardScreen
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.SyllabusScreen
import com.example.ui.SessionScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.receiver.TimerService

class MainActivity : ComponentActivity() {

  companion object {
    val notificationClickTrigger = kotlinx.coroutines.flow.MutableStateFlow(0L)
  }

  private val app by lazy { application as MedPrepApplication }
  private val viewModelFactory by lazy { MainViewModelFactory(app.repository, app.backupManager) }
  private val viewModel: MainViewModel by viewModels { viewModelFactory }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    if (intent?.action == "SHOW_TIMER") {
        notificationClickTrigger.value = System.currentTimeMillis()
    }
    
    setContent {
      MyApplicationTheme {
        MainAppScreen(viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    try {
        com.example.receiver.TodayStudyWidget.updateAllWidgets(this)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.action == "SHOW_TIMER") {
        notificationClickTrigger.value = System.currentTimeMillis()
    }
  }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"
    
    val currentBaseRoute = currentRoute.substringBefore("?")
    val showNav = currentBaseRoute != "timer" && currentBaseRoute != "session_setup"

    val isServiceRunning by TimerService.isServiceRunning.collectAsState()
    val notificationTrigger by MainActivity.notificationClickTrigger.collectAsState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    LaunchedEffect(isServiceRunning, notificationTrigger, currentBaseRoute) {
        if (isServiceRunning && currentBaseRoute != "timer") {
            val subject = TimerService.timerSubjectState.value
            val chapter = TimerService.timerChapterState.value
            val type = TimerService.timerSessionTypeState.value

            val encSubject = android.net.Uri.encode(subject)
            val encChapter = android.net.Uri.encode(chapter)
            val encType = android.net.Uri.encode(type)

            navController.navigate("timer?subject=$encSubject&chapter=$encChapter&type=$encType") {
                popUpTo("dashboard") { saveState = false }
            }
        }
    }

    val navItems = listOf(
        Triple("dashboard", "ড্যাশবোর্ড", Icons.Default.Dashboard),
        Triple("syllabus", "সিলেবাস", Icons.Default.MenuBook),
        Triple("session", "সেশন", Icons.Default.HourglassEmpty),
        Triple("analysis", "বিশ্লেষণ", Icons.Default.Analytics),
        Triple("system", "সিস্টেম", Icons.Default.Settings)
    )

    if (isTablet && showNav) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            NavigationRail(
                containerColor = Color(0xFF0F172A),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                header = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .size(40.dp)
                            .background(Color(0xFFFAFAFB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_custom_logo),
                            contentDescription = "Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            ) {
                navItems.forEach { (route, label, icon) ->
                    val selected = currentRoute == route
                    NavigationRailItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        selected = selected,
                        onClick = {
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AppNavGraph(navController, viewModel)
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBackground,
            bottomBar = {
                if (showNav) {
                    NavigationBar(
                        containerColor = DarkBackground,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        navItems.forEach { (route, label, icon) ->
                            val selected = currentRoute == route
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                AppNavGraph(navController, viewModel)
            }
        }
    }
}

@Composable
private fun AppNavGraph(
    navController: androidx.navigation.NavHostController,
    viewModel: MainViewModel
) {
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen(viewModel) }
        composable("syllabus") { 
            SyllabusScreen(viewModel, onStartChapter = { subject, chapter ->
                val encSubject = android.net.Uri.encode(subject)
                val encChapter = android.net.Uri.encode(chapter)
                navController.navigate("session_setup?subject=$encSubject&chapter=$encChapter")
            }) 
        }
        composable("analysis") { AnalysisScreen(viewModel) }
        
        composable("session") { 
            SessionScreen(
                viewModel = viewModel, 
                subject = "সাধারণ", 
                chapter = "সাধারণ সেশন",
                onNavigateToTimer = { type ->
                    val encSubject = android.net.Uri.encode("সাধারণ")
                    val encChapter = android.net.Uri.encode("সাধারণ সেশন")
                    val encType = android.net.Uri.encode(type)
                    navController.navigate("timer?subject=$encSubject&chapter=$encChapter&type=$encType") {
                        popUpTo("dashboard")
                    }
                },
                onClose = { navController.navigate("dashboard") { popUpTo(0) } }
            ) 
        }
        
        composable(
            "session_setup?subject={subject}&chapter={chapter}",
            arguments = listOf(
                navArgument("subject") { type = NavType.StringType; defaultValue = "" },
                navArgument("chapter") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject")?.replace("+", " ") ?: ""
            val chapter = backStackEntry.arguments?.getString("chapter")?.replace("+", " ") ?: ""
            SessionScreen(
                viewModel = viewModel,
                subject = subject,
                chapter = chapter,
                onNavigateToTimer = { type ->
                    val encSubject = android.net.Uri.encode(subject)
                    val encChapter = android.net.Uri.encode(chapter)
                    val encType = android.net.Uri.encode(type)
                    navController.navigate("timer?subject=$encSubject&chapter=$encChapter&type=$encType") {
                        popUpTo("dashboard")
                    }
                },
                onClose = { navController.popBackStack() }
            )
        }
        
        composable(
            "timer?subject={subject}&chapter={chapter}&type={type}",
            arguments = listOf(
                navArgument("subject") { type = NavType.StringType; defaultValue = "" },
                navArgument("chapter") { type = NavType.StringType; defaultValue = "" },
                navArgument("type") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject")?.replace("+", " ") ?: ""
            val chapter = backStackEntry.arguments?.getString("chapter")?.replace("+", " ") ?: ""
            val type = backStackEntry.arguments?.getString("type")?.replace("+", " ") ?: ""
            
            TimerScreen(
                viewModel = viewModel,
                subject = subject,
                chapter = chapter,
                sessionType = type,
                onStop = {
                    navController.navigate("analysis") {
                        popUpTo("dashboard")
                    }
                }
            )
        }
        
        composable("system") { com.example.ui.SystemScreen(viewModel) }
        composable("code") { com.example.ui.SystemScreen(viewModel) }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize().background(DarkBackground), contentAlignment = Alignment.Center) {
        Text(title, color = MaterialTheme.colorScheme.onBackground)
    }
}


