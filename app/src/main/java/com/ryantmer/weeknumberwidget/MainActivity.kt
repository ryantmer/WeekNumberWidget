package com.ryantmer.weeknumberwidget

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        val weekNumberView = findViewById<TextView>(R.id.week_number)
        weekNumberView.text = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR).toString()
    }
}