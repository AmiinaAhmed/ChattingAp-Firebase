package com.example.umechatting.About;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.umechatting.R;

public class AboutAppActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private Button gitBtn, InstaBtn, TwBtn, LinBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_app);

        // Set Home Activity Toolbar Name
        mToolbar = findViewById(R.id.about_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("About");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);






    }

}
