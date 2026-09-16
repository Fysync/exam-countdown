package com.example.examcountdown

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.examcountdown.databinding.ActivityMainBinding

/**
 * 应用入口：简单的安装/添加小组件说明。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}