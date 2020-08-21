package com.example.umechatting.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umechatting.Home.MainActivity;
import com.example.umechatting.Model.Message;
import com.example.umechatting.R;
import com.example.umechatting.Show.ImageViewerActivity;
import com.example.umechatting.Show.Video_viewerActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>{

    private List<Message> messageList;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private Context mcontent;
    private Runnable runnable;
    private Handler handler1=new Handler();
    public MessageAdapter(List<Message> messageList,Context mcontent) {
        this.messageList = messageList;
        this.mcontent=mcontent;
    }


    @Override
    public MessageViewHolder onCreateViewHolder( ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_message_layout, parent, false);
        mAuth = FirebaseAuth.getInstance();
        return new MessageViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onBindViewHolder(final MessageViewHolder holder,final int position) {
        String sender_UID = mAuth.getCurrentUser().getUid();
        final Message message = messageList.get(position);

        String from_user_ID = message.getFrom();
        String from_message_TYPE = message.getType();

        databaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(from_user_ID);
        databaseReference.keepSynced(true); // for offline
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String userName = dataSnapshot.child("user_name").getValue().toString();
                    String userProfileImage = dataSnapshot.child("user_thumb_image").getValue().toString();
                    //
                    Picasso.get()
                            .load(userProfileImage)
                            .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                            .placeholder(R.drawable.default_profile_image)
                            .into(holder.user_profile_image);
                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // when msg is TEXT, image views are gone
        holder.senderImageMsg.setVisibility(View.GONE);
        holder.receiverImageMsg.setVisibility(View.GONE);
        holder.user_profile_image.setVisibility(View.INVISIBLE);
        holder.sender_text_message.setVisibility(View.GONE);
        holder.receiver_text_message.setVisibility(View.GONE);
        holder.messagesenderVideo.setVisibility(View.GONE);
        holder.messagereceiverVideo.setVisibility(View.GONE);
        holder.receivervoicerecor.setVisibility(View.GONE);
        holder.sendervoicerecor.setVisibility(View.GONE);
        holder.receivercontrol.setVisibility(View.GONE);
        holder.sendercontrol.setVisibility(View.GONE);

        // if message type is TEXT
        if (from_message_TYPE.equals("text")){


            if (from_user_ID.equals(sender_UID)){
                holder.sender_text_message.setBackgroundResource(R.drawable.single_message_text_another_background);
                holder.sender_text_message.setTextColor(Color.BLACK);
                holder.sender_text_message.setGravity(Gravity.LEFT);
                holder.sender_text_message.setText(message.getMessage());
            } else {
                holder.sender_text_message.setVisibility(View.INVISIBLE);
                holder.receiver_text_message.setVisibility(View.VISIBLE);
                holder.user_profile_image.setVisibility(View.VISIBLE);

                holder.receiver_text_message.setBackgroundResource(R.drawable.single_message_text_background);
                holder.receiver_text_message.setTextColor(Color.WHITE);
                holder.receiver_text_message.setGravity(Gravity.LEFT);
                holder.receiver_text_message.setText(message.getMessage());
            }
        }
        if (from_message_TYPE.equals("image")){

            if (from_user_ID.equals(sender_UID)){
                holder.senderImageMsg.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(message.getMessage())
                        .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                         //.placeholder(R.drawable.default_profile_image)
                        .into(holder.senderImageMsg);
                Log.e("tag","from adapter, link : "+ message.getMessage());
            } else {
                holder.user_profile_image.setVisibility(View.VISIBLE);
                holder.receiverImageMsg.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(message.getMessage())
                        .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                         //.placeholder(R.drawable.default_profile_image)
                        .into(holder.receiverImageMsg);
                Log.e("tag","from adapter, link : "+ message.getMessage());

            }
        }
        if (from_message_TYPE.equals("video")){
            if(from_user_ID.equals(sender_UID)){
                holder.messagesenderVideo.setVisibility(View.VISIBLE);
                Uri uri=Uri.parse(message.getMessage());
                holder.messagesenderVideo.setVideoURI(uri);
                holder.messagesenderVideo.requestFocus();
                holder.messagesenderVideo.setMediaController(new MediaController(mcontent));

            }else{
                holder.user_profile_image.setVisibility(View.VISIBLE);
                holder.messagereceiverVideo.setVisibility(View.VISIBLE);
                Uri uri=Uri.parse(message.getMessage());
                holder.messagereceiverVideo.setVideoURI(uri);
                holder.messagereceiverVideo.requestFocus();
                holder.messagereceiverVideo.setMediaController(new MediaController(mcontent));
            }

        }
        if (from_message_TYPE.equals("pdf")){
            if(from_user_ID.equals(sender_UID)){
                holder.senderImageMsg.setVisibility(View.VISIBLE);
                holder.senderImageMsg.setBackgroundResource(R.mipmap.ic_pdf);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(messageList.get(position).getMessage()));
                        holder.itemView.getContext().startActivity(intent);
                    }
                });
            }else{
                holder.user_profile_image.setVisibility(View.VISIBLE);
                holder.receiverImageMsg.setVisibility(View.VISIBLE);
                holder.receiverImageMsg.setBackgroundResource(R.mipmap.ic_pdf);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(messageList.get(position).getMessage()));
                        holder.itemView.getContext().startActivity(intent);
                    }
                });
            }
        }
        if (from_message_TYPE.equals("map")){
            if(from_user_ID.equals(sender_UID)){
               holder.senderImageMsg.setVisibility(View.VISIBLE);
                Picasso.get().load(message.getMessage()).into(holder.senderImageMsg);

            }else{
               holder.user_profile_image.setVisibility(View.VISIBLE);
                holder.receiverImageMsg.setVisibility(View.VISIBLE);
               Picasso.get().load(message.getMessage()).into(holder.receiverImageMsg);
            }

        }
        if(from_message_TYPE.equals("Audio")){
            final MediaPlayer mediaPlayer = new MediaPlayer();
            if(from_user_ID.equals(sender_UID)){
                holder.sendercontrol.setVisibility(View.VISIBLE);
                holder.sendervoicerecor.setVisibility(View.VISIBLE);
                holder.sendervoicerecor.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                // messageViewHolder.sendervoicerecor.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                holder.sendercontrol.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            mediaPlayer.reset();
                            mediaPlayer.setDataSource(message.getMessage());
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.prepare();
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    holder.sendervoicerecor.setMax(mediaPlayer.getDuration());
                                    holder.sendercontrol.setImageResource(R.mipmap.ic_start);
                                    mp.start();
                                    changeBar(holder.sendervoicerecor,mediaPlayer,holder.sendercontrol);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });


            }else{
                holder.user_profile_image.setVisibility(View.VISIBLE);
                holder.receivercontrol.setVisibility(View.VISIBLE);
                holder.receivervoicerecor.setVisibility(View.VISIBLE);
                holder.receivervoicerecor.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                // messageViewHolder.sendervoicerecor.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                holder.receivercontrol.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            mediaPlayer.reset();
                            mediaPlayer.setDataSource(message.getMessage());
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.prepare();
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    holder.receivervoicerecor.setMax(mediaPlayer.getDuration());
                                    holder.receivercontrol.setImageResource(R.mipmap.ic_start);
                                    mp.start();
                                    changeBar(holder.receivervoicerecor, mediaPlayer, holder.receivercontrol);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        } }
                });
            }

        }
        else if(from_message_TYPE.equals("docx")){
            if(from_user_ID.equals(sender_UID)){
                holder.senderImageMsg.setVisibility(View.VISIBLE);
                holder.senderImageMsg.setBackgroundResource(R.mipmap.ic_file);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(messageList.get(position).getMessage()));
                        holder.itemView.getContext().startActivity(intent);
                    }
                });
            }else{
                holder.user_profile_image.setVisibility(View.VISIBLE);
                holder.receiverImageMsg.setVisibility(View.VISIBLE);
                holder.receiverImageMsg.setBackgroundResource(R.mipmap.ic_file);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(messageList.get(position).getMessage()));
                        holder.itemView.getContext().startActivity(intent);
                    }
                });
            }
        }
        if(from_user_ID.equals(sender_UID)){
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(messageList.get(position).getType().equals("pdf") ||messageList.get(position).getType().equals("docx") ){
                        CharSequence options[]=new CharSequence[]{
                                "Delete For Me",
                                "Download this Document",
                                "Cancel",
                                "Delete for Everyone"
                        };
                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Message");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(i==0){
                                    deleteSendmessage(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else if(i==1){
                                    Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(messageList.get(position).getMessage()));
                                    holder.itemView.getContext().startActivity(intent);

                                }
                                else if(i==3){
                                    deletemessageForevery(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                    else if(messageList.get(position).getType().equals("text")){
                        CharSequence options[]=new CharSequence[]{
                                "Delete For Me",
                                "Cancel",
                                "Delete for Everyone"
                        };
                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Message");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(i==0){
                                    deleteSendmessage(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }

                                else if(i==2){
                                    deletemessageForevery(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                    else if(messageList.get(position).getType().equals("image")){
                        CharSequence options[]=new CharSequence[]{
                                "Delete For Me",
                                "View image",
                                "Cancel",
                                "Delete for Everyone"
                        };
                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Message");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(i==0){
                                    deleteSendmessage(position , holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    intent.putExtra("url",messageList.get(position).getMessage());
                                }
                                else if(i==1){
                                    Intent intent=new Intent(holder.itemView.getContext(), ImageViewerActivity.class);
                                    intent.putExtra("url",messageList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else if(i==3){
                                    deletemessageForevery(position , holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                    if(messageList.get(position).getType().equals("video")){
                        CharSequence options[]=new CharSequence[]{
                                "Delete For Me",
                                "Open this Video",
                                "Cancel",
                                "Delete for Everyone"
                        };
                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Message");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(i==0){
                                    deleteSendmessage(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else if(i==1){
                                    Intent intent=new Intent(holder.itemView.getContext(), Video_viewerActivity.class);
                                    intent.putExtra("url",messageList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else if(i==3){
                                    deletemessageForevery(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                }
            });

        }
        else{
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(messageList.get(position).getType().equals("pdf") ||messageList.get(position).getType().equals("docx") ){
                        CharSequence options[]=new CharSequence[]{
                                "Delete For Me",
                                "Download this Document",
                                "Cancel",

                        };
                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("__Message__");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(i==0){
                                    deleteRecmessage(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else if(i==1){

                                    Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(messageList.get(position).getMessage()));
                                    holder.itemView.getContext().startActivity(intent);
                                }

                            }
                        });
                        builder.show();
                    }
                    else if(messageList.get(position).getType().equals("text")){
                        CharSequence options[]=new CharSequence[]{
                                "Delete For Me",
                                "Cancel",
                        };
                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("__Message__");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(i==0){
                                    deleteRecmessage(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                    else if(messageList.get(position).getType().equals("image")){
                        CharSequence options[]=new CharSequence[]{
                                "Delete For Me",
                                "View image",
                                "Cancel"
                        };
                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("__Message__");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(i==0){
                                    deleteRecmessage(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else if(i==1){
                                    Intent intent=new Intent(holder.itemView.getContext(),ImageViewerActivity.class);
                                    intent.putExtra("url",messageList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);

                                }

                            }
                        });
                        builder.show();
                    } else if(messageList.get(position).getType().equals("video")){
                        CharSequence options[]=new CharSequence[]{
                                "Delete For Me",
                                "View video",
                                "Cancel"
                        };
                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Message");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(i==0){
                                    deleteRecmessage(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else if(i==1){
                                    Intent intent=new Intent(holder.itemView.getContext(),Video_viewerActivity.class);
                                    intent.putExtra("url",messageList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);

                                }

                            }
                        });
                        builder.show();
                    }
                }
            });

        }

    }
    private void changeBar(final SeekBar seekBar, final MediaPlayer mediaPlayer , final ImageView but){
        seekBar.setProgress(mediaPlayer.getCurrentPosition());
        if(mediaPlayer.isPlaying()){
            runnable=new Runnable() {
                @Override
                public void run() {
                    changeBar(seekBar,mediaPlayer,but);
                }
            };
            handler1.postDelayed(runnable,1000);
        }
        else
            but.setImageResource(R.mipmap.ic_stop);

    }
    private void deleteSendmessage(final int postion , final MessageViewHolder holder){
        DatabaseReference rootRef=FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages")
                .child(messageList.get(postion).getFrom())
                .child(messageList.get(postion).getTo())
                .child(messageList.get(postion).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(holder.itemView.getContext(),"Deleted Successful",Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(holder.itemView.getContext(),"error Occured",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
    private void deleteRecmessage(final int postion , final MessageViewHolder holder){
        DatabaseReference rootRef=FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages")
                .child(messageList.get(postion).getTo())
                .child(messageList.get(postion).getFrom())
                .child(messageList.get(postion).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(holder.itemView.getContext(),"Deleted Successful",Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(holder.itemView.getContext(),"error Occured",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
    private void deletemessageForevery(final int postion , final MessageViewHolder holder){
        final DatabaseReference rootRef=FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages")
                .child(messageList.get(postion).getTo())
                .child(messageList.get(postion).getFrom())
                .child(messageList.get(postion).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    rootRef.child("Messages")
                            .child(messageList.get(postion).getFrom())
                            .child(messageList.get(postion).getTo())
                            .child(messageList.get(postion).getMessageID())
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(holder.itemView.getContext(),"Deleted Successful",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else{
                    Toast.makeText(holder.itemView.getContext(),"error Occured",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder{
        TextView sender_text_message, receiver_text_message;
        CircleImageView user_profile_image;
        RoundedImageView senderImageMsg, receiverImageMsg;
        public VideoView messagereceiverVideo,messagesenderVideo;
        public SeekBar sendervoicerecor,receivervoicerecor;
        public ImageView sendercontrol , receivercontrol;

        MessageViewHolder(View view){
            super(view);
            sender_text_message = view.findViewById(R.id.senderMessageText);
            receiver_text_message = view.findViewById(R.id.receiverMessageText);
            user_profile_image = view.findViewById(R.id.messageUserImage);

            senderImageMsg = view.findViewById(R.id.messageImageVsender);
            receiverImageMsg = view.findViewById(R.id.messageImageVreceiver);
            messagesenderVideo=(VideoView)itemView.findViewById(R.id.sender_message_video);
            messagereceiverVideo=(VideoView)itemView.findViewById(R.id.receiver_message_video);
            sendervoicerecor=(SeekBar) itemView.findViewById(R.id.sender_message_record);
            receivervoicerecor=(SeekBar) itemView.findViewById(R.id.receiver_message_record);
            receivercontrol=(ImageView)itemView.findViewById(R.id.receiver_control);
            sendercontrol=(ImageView)itemView.findViewById(R.id.sender_control);
        }

    }
}
