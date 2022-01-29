package il.ronmad.speedruntimer.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mukesh.MarkdownView
import il.ronmad.speedruntimer.R

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val markdownView = findViewById<MarkdownView>(R.id.markdownView)
        markdownView.loadMarkdownFromAssets("help.md")
        markdownView.isOpenUrlInBrowser = true
    }
}
