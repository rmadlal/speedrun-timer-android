package il.ronmad.speedruntimer.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import il.ronmad.speedruntimer.R
import kotlinx.android.synthetic.main.activity_help.*

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        markdownView.loadMarkdownFromAssets("help.md")
        markdownView.isOpenUrlInBrowser = true
    }
}
