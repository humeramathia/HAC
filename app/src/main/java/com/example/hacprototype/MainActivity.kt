// ========================================
// START OF CODE
// ========================================

package com.example.hacprototype

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

/**
 * Single-activity host for the Habibia prototype.
 *
 * Every screen is a fragment swapped into [R.id.fragmentContainer]. The activity
 * does not use the fragment back stack; [OnBackPressedCallback] encodes the
 * role-aware exits instead.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                when (current) {
                    is MemberHostFragment, is LoginFragment, is SplashFragment -> finish()
                    is AdminDashboardFragment -> navigateTo(LoginFragment())
                    else -> {
                        if (HabibiaSession.isAdmin) {
                            navigateTo(AdminDashboardFragment())
                        } else {
                            navigateTo(MemberHostFragment())
                        }
                    }
                }
            }
        })

        // Splash only on a fresh launch so rotation does not replay the delay.
        if (savedInstanceState == null) {
            navigateTo(SplashFragment())
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing) navigateTo(LoginFragment())
            }, 1100)
        }
    }

    /**
     * Replaces the current screen. Transactions are not added to the back stack
     * because back is handled explicitly above.
     */
    fun navigateTo(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}

// ========================================
// END OF CODE
// ========================================
