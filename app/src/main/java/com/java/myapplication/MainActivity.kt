package com.java.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.java.myapplication.app.MojiangApp
import com.java.myapplication.data.LocalNovelStore
import com.java.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalNovelStore.init(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MojiangApp()
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppPreview() {
    MyApplicationTheme {
        MojiangApp()
    }
}