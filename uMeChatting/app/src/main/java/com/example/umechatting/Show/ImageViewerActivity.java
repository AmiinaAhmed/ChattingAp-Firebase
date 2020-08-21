package com.example.umechatting.Show;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.umechatting.R;
import com.squareup.picasso.Picasso;

public class ImageViewerActivity extends AppCompatActivity {

    private ImageView imageView;
    private String imageurl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        imageView=findViewById(R.id.image_view_pager);
        imageurl=getIntent().getStringExtra("url");
        Picasso.get().load(imageurl).into(imageView);
    }
}

