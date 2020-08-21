package com.example.umechatting.Show;

import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.umechatting.R;

public class Video_viewerActivity extends AppCompatActivity {

    private VideoView videoView;
    private String videourl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_viewer);

        videoView=(VideoView)findViewById(R.id.video_viewer);
        videourl=getIntent().getStringExtra("url");
        videoView.setVideoURI(Uri.parse(videourl));
        videoView.setMediaController(new MediaController(Video_viewerActivity.this));
    }
}
