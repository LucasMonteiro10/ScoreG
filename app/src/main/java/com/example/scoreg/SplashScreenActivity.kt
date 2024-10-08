package com.example.scoreg

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.example.scoreg.models.MainViewModel
import com.example.scoreg.utils.IntentUtils

class SplashScreenActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val ivNote: ImageView = findViewById(R.id.iv_note)
        ivNote.alpha = 0f;
        ivNote.animate().setDuration(1500).alpha(1f).withEndAction {
            if(viewModel.loggedIn){
                val intent = IntentUtils.createMainActivityIntent(this)
                startActivity(intent)
            }else{
                val intent = IntentUtils.createLoginActivityIntent(this)
                startActivity(intent)
            }

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }
}