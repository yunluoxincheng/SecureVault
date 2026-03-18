package com.securevault.ui.navigation

import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.securevault.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LoginBackStackRegressionTest {

    @Test
    fun lock_to_login_back_should_not_return_to_vault() {
        val navReady = CountDownLatch(1)
        var navController: NavHostController? = null

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    val controller = rememberNavController()
                    navController = controller
                    navReady.countDown()
                    NavHost(navController = controller, startDestination = "vault") {
                        composable("vault") {}
                        composable("settings") {}
                        composable("login") {}
                    }
                }
            }

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertTrue(navReady.await(5, TimeUnit.SECONDS))

            scenario.onActivity {
                val controller = navController
                assertNotNull(controller)
                controller!!.navigate("settings")
                controller.navigateToLoginAfterLock(
                    loginRoute = "login",
                    vaultRoute = "vault"
                )

                val canGoBack = controller.popBackStack()
                assertFalse(canGoBack)
            }
        }
    }

    @Test
    fun register_to_login_back_should_not_return_to_register() {
        val navReady = CountDownLatch(1)
        var navController: NavHostController? = null

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    val controller = rememberNavController()
                    navController = controller
                    navReady.countDown()
                    NavHost(navController = controller, startDestination = "register") {
                        composable("register") {}
                        composable("login") {}
                    }
                }
            }

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertTrue(navReady.await(5, TimeUnit.SECONDS))

            scenario.onActivity {
                val controller = navController
                assertNotNull(controller)
                controller!!.navigateToLoginFromRegister(
                    loginRoute = "login",
                    registerRoute = "register"
                )

                val canGoBack = controller.popBackStack()
                assertFalse(canGoBack)
            }
        }
    }
}
