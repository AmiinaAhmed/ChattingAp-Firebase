package com.example.umechatting.Chat;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umechatting.Adapter.MessageAdapter;
import com.example.umechatting.Model.MapModel;
import com.example.umechatting.Model.Message;
import com.example.umechatting.R;
import com.example.umechatting.Utils.UserLastSeenTime;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.hdodenhof.circleimageview.CircleImageView;



public class ChatActivity extends AppCompatActivity {

    private String messageReceiverID;
    private String messageReceiverName;
    private String myuri = "";
    private Toolbar chatToolbar;
    private TextView chatUserName;
    private TextView chatUserActiveStatus, ChatConnectionTV;
    private CircleImageView chatUserImageView;

    private DatabaseReference rootReference;

    // sending message
    private ImageView send_message, send_record;
    private EditText input_user_message;
    private FirebaseAuth mAuth;
    private String messageSenderId, download_url;
    private String checker = "";
    private RecyclerView messageList_ReCyVw;
    private final List<Message> messageList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;

    private final static int GALLERY_PICK_CODE = 2;
    private StorageReference imageMessageStorageRef;
    private int IMAGE_GALLERY_REQUEST = 965;
    private int CAMERA_REQ = 960;
    private int video_GALLERY_REQUEST = 700;
    private int PLACE_PICKER_REQUEST = 600;
    private Uri fileUri;
    private StorageTask uploadTask;
    private ProgressDialog loadingbar;

    private File filePathImageCamera;
    private MediaRecorder mediaRecorder;
    private MediaPlayer player = null;
    private File fileName;
    private final static String LOG_TAG = "Record_log";
    private ImageButton record_voice;
    private ConnectivityReceiver connectivityReceiver;

    AlertDialog.Builder alertDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rootReference = FirebaseDatabase.getInstance().getReference();

        mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser().getUid();

        messageReceiverID = getIntent().getExtras().get("visitUserId").toString();
        messageReceiverName = getIntent().getExtras().get("userName").toString();

        imageMessageStorageRef = FirebaseStorage.getInstance().getReference().child("messages_image");

        // appbar / toolbar
        chatToolbar = findViewById(R.id.chats_appbar);
        setSupportActionBar(chatToolbar);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        String nomeFoto = DateFormat.format("yyyy-MM-dd_hhmmss", new Date()).toString();
        fileName = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), nomeFoto + "record.3gp");


        alertDialog = new AlertDialog.Builder(ChatActivity.this);
        LayoutInflater layoutInflater = (LayoutInflater)
                this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.appbar_chat, null);
        actionBar.setCustomView(view);
        loadingbar = new ProgressDialog(this);
        ChatConnectionTV = findViewById(R.id.ChatConnectionTV);
        chatUserName = findViewById(R.id.chat_user_name);
        chatUserActiveStatus = findViewById(R.id.chat_active_status);
        chatUserImageView = findViewById(R.id.chat_profile_image);

        // sending message declaration
        send_message = findViewById(R.id.c_send_message_BTN);
        send_record = findViewById(R.id.c_send_voice_BTN);
        input_user_message = findViewById(R.id.c_input_message);

        // setup for showing messages
        messageAdapter = new MessageAdapter(messageList,this);
        messageList_ReCyVw = findViewById(R.id.message_list);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageList_ReCyVw.setLayoutManager(linearLayoutManager);
        messageList_ReCyVw.setHasFixedSize(true);
        //linearLayoutManager.setReverseLayout(true);
        messageList_ReCyVw.setAdapter(messageAdapter);

        fetchMessages();

        chatUserName.setText(messageReceiverName);
        rootReference.child("users").child(messageReceiverID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        final String active_status = dataSnapshot.child("active_now").getValue().toString();
                        final String thumb_image = dataSnapshot.child("user_thumb_image").getValue().toString();

//                        // FOR TESTING
//                        if (currentUser != null){
//                            rootReference.child("active_now").setValue(ServerValue.TIMESTAMP);
//                        }

                        // show image on appbar
                        Picasso.get()
                                .load(thumb_image)
                                .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                                .placeholder(R.drawable.default_profile_image)
                                .into(chatUserImageView, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                    }
                                    @Override
                                    public void onError(Exception e) {
                                        Picasso.get()
                                                .load(thumb_image)
                                                .placeholder(R.drawable.default_profile_image)
                                                .into(chatUserImageView);
                                    }
                                });

                        //active status
                        if (active_status.contains("true")){
                            chatUserActiveStatus.setText("Active now");
                        } else {
                            UserLastSeenTime lastSeenTime = new UserLastSeenTime();
                            long last_seen = Long.parseLong(active_status);

                            //String lastSeenOnScreenTime = lastSeenTime.getTimeAgo(last_seen).toString();
                            String lastSeenOnScreenTime = lastSeenTime.getTimeAgo(last_seen, getApplicationContext()).toString();
                            Log.e("lastSeenTime", lastSeenOnScreenTime);
                            if (lastSeenOnScreenTime != null){
                                chatUserActiveStatus.setText(lastSeenOnScreenTime);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

        rootReference.child("Messages").child(messageSenderId).child(messageReceiverID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Message messages = dataSnapshot.getValue(Message.class);

                        messageList.add(messages);

                        messageAdapter.notifyDataSetChanged();

                        messageList_ReCyVw.smoothScrollToPosition(messageList_ReCyVw.getAdapter().getItemCount());
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        /**
         *  SEND TEXT MESSAGE BUTTON
         */
        send_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        send_record.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == motionEvent.ACTION_DOWN) {

                    input_user_message.setText("Recording Started ...");
                    startRecording();

                } else if (motionEvent.getAction() == motionEvent.ACTION_UP) {
                    input_user_message.setText("Recording stoped ...");
                    // Setting Dialog Message
                    alertDialog.setMessage("Do you want to save Record?");
                    alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            input_user_message.setText("");
                        }
                    });
                    alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            stopRecord();
                        }
                    });

                    // Showing Alert Message
                    alertDialog.show();

                }
                return false;
            }
        });


    } // ending onCreate

    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(fileName.toString());
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        mediaRecorder.start();
    }

    private void stopRecord() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        uploadvoice();
    }
    private void uploadvoice() {

        input_user_message.setText("");
        loadingbar.setTitle("Upload voice");
        loadingbar.setMessage("Loading");
        loadingbar.setCanceledOnTouchOutside(false);
        loadingbar.show();

        fileUri = Uri.fromFile(fileName);
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Audio");
        final String messageSenderRef = "Messages/" + messageSenderId + "/" + messageReceiverID;
        final String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderId;

        DatabaseReference userMessageKeyRef = rootReference.child("Messages")
                .child(messageSenderId).child(messageReceiverID).push();
        final String messagePushID = userMessageKeyRef.getKey();
        final StorageReference filepath = storageReference.child(messagePushID + "." + "Audio");
        filepath.putFile(fileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete( Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    final Map messageTextBody = new HashMap();
                    filepath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override

                        public void onComplete( Task<Uri> task) {

                            final String downloadUrl = task.getResult().toString();
                            messageTextBody.put("message", downloadUrl);
                            messageTextBody.put("name", fileUri.getLastPathSegment());
                            messageTextBody.put("type", "Audio");
                            messageTextBody.put("from", messageSenderId);
                            messageTextBody.put("to", messageReceiverID);
                            messageTextBody.put("messageID", messagePushID);
                            messageTextBody.put("time", ServerValue.TIMESTAMP);

                            Map messageBodyDetails = new HashMap();
                            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
                            messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);
                            rootReference.updateChildren(messageBodyDetails);
                            loadingbar.dismiss();

                        }
                    });
                }
            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure( Exception e) {
                loadingbar.dismiss();
                Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double p = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                loadingbar.setMessage((int) p + "% Uploading...");
            }
        });

    }
    @Override
    protected void onResume() {
        super.onResume();
        //Register Connectivity Broadcast receiver
        connectivityReceiver = new ConnectivityReceiver();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, intentFilter);
    }
    @Override
    protected void onStop() {
        super.onStop();
        // Unregister Connectivity Broadcast receiver
        unregisterReceiver(connectivityReceiver);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.messages_types_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.Image_Mssg) {
            checker = "image";
            Intent img = new Intent();
            img.setAction(Intent.ACTION_GET_CONTENT);
            img.setType("image/*");
            startActivityForResult(img.createChooser(img, "Select Image"), IMAGE_GALLERY_REQUEST);
        }

        if (item.getItemId() == R.id.Video_mssg) {
            checker = "video";
            Intent img = new Intent();
            img.setAction(Intent.ACTION_GET_CONTENT);
            img.setType("video/*");
            startActivityForResult(img.createChooser(img, "Select video"), video_GALLERY_REQUEST);
        }

        if (item.getItemId() == R.id.Pdf_file) {
            checker = "pdf";
            Intent img = new Intent();
            img.setAction(Intent.ACTION_GET_CONTENT);
            img.setType("application/pdf");
            startActivityForResult(img.createChooser(img, "Select pdf file"), IMAGE_GALLERY_REQUEST);
        }

        if (item.getItemId() == R.id.txt_File) {
            checker = "docx";
            Intent img = new Intent();
            img.setAction(Intent.ACTION_GET_CONTENT);
            img.setType("application/msword");
            startActivityForResult(img.createChooser(img, "Select Ms Word"), IMAGE_GALLERY_REQUEST);
        }

        if (item.getItemId() == R.id.Camara_mssg) {
            String nomeFoto = DateFormat.format("yyyy-MM-dd_hhmmss", new Date()).toString();
            filePathImageCamera = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), nomeFoto + "camera.jpg");
            checker = "image";
            Intent it = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // startActivityForResult(img.createChooser(img,"Select Image"),IMAGE_GALLERY_REQUEST);
            if (it.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(it.createChooser(it, "Select Image"), CAMERA_REQ);
            }
        }
        if (item.getItemId() == R.id.share_loc) {
            checker = "map";
            try {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                startActivityForResult(builder.build(ChatActivity.this), PLACE_PICKER_REQUEST);
            } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                Toast.makeText(ChatActivity.this,"1222"+e.getMessage(),Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
        return false;
    }


    private void fetchMessages() {
        rootReference.child("Messages").child(messageSenderId).child(messageReceiverID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        if (dataSnapshot.exists()){
                            Message message = dataSnapshot.getValue(Message.class);
                            messageList.add(message);
                            messageAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    }
                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                    }
                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }



    private void sendMessage() {
        String message = input_user_message.getText().toString();
        if (TextUtils.isEmpty(message)){

            Toast.makeText(ChatActivity.this, "Please type a message",Toast.LENGTH_LONG).show();
        } else {
            String message_sender_reference = "Messages/" + messageSenderId + "/" + messageReceiverID;
            String message_receiver_reference = "Messages/" + messageReceiverID + "/" + messageSenderId;

            DatabaseReference user_message_key = rootReference.child("Messages").child(messageSenderId).child(messageReceiverID).push();
            String message_push_id = user_message_key.getKey();

            HashMap<String, Object> message_text_body = new HashMap<>();
            message_text_body.put("message", message);
            message_text_body.put("seen", false);
            message_text_body.put("type", "text");
            message_text_body.put("time", ServerValue.TIMESTAMP);
            message_text_body.put("from", messageSenderId);
            message_text_body.put("to", messageReceiverID);

            HashMap<String, Object> messageBodyDetails = new HashMap<>();
            messageBodyDetails.put(message_sender_reference + "/" + message_push_id, message_text_body);
            messageBodyDetails.put(message_receiver_reference + "/" + message_push_id, message_text_body);

            rootReference.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                    if (databaseError != null){
                        Log.e("Sending message", databaseError.getMessage());
                    }
                    input_user_message.setText("");
                }
            });
        }
    }


    // Broadcast receiver for network checking
    public class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            ChatConnectionTV.setVisibility(View.GONE);
            if (networkInfo != null && networkInfo.isConnected()){
                ChatConnectionTV.setText("Internet connected");
                ChatConnectionTV.setTextColor(Color.WHITE);
                ChatConnectionTV.setVisibility(View.VISIBLE);

                // LAUNCH activity after certain time period
                new Timer().schedule(new TimerTask(){
                    public void run() {
                        ChatActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                ChatConnectionTV.setVisibility(View.GONE);
                            }
                        });
                    }
                }, 1200);
            } else {
                ChatConnectionTV.setText("No internet connection! ");
                ChatConnectionTV.setTextColor(Color.WHITE);
                ChatConnectionTV.setBackgroundColor(Color.RED);
                ChatConnectionTV.setVisibility(View.VISIBLE);
            }
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_GALLERY_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                fileUri = data.getData();
                if (fileUri != null) {
                    loadingbar.setTitle("Send file");
                    loadingbar.setMessage("Loading");
                    loadingbar.setCanceledOnTouchOutside(false);
                    loadingbar.show();

                    if (!checker.equals("image")) {
                        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Folder");
                        final String messageSenderRef = "Messages/" + messageSenderId + "/" + messageReceiverID;
                        final String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderId;

                        DatabaseReference userMessageKeyRef = rootReference.child("Messages")
                                .child(messageSenderId).child(messageReceiverID).push();
                        final String messagePushID = userMessageKeyRef.getKey();
                        final StorageReference filepath = storageReference.child(messagePushID + "." + checker);
                        filepath.putFile(fileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete( Task<UploadTask.TaskSnapshot> task) {
                                if (task.isSuccessful()) {
                                    final Map messageTextBody = new HashMap();
                                    filepath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                        @Override

                                        public void onComplete( Task<Uri> task) {


                                            final String downloadUrl = task.getResult().toString();
                                            messageTextBody.put("message", downloadUrl);
                                            messageTextBody.put("name", fileUri.getLastPathSegment());
                                            messageTextBody.put("type", checker);
                                            messageTextBody.put("from", messageSenderId);
                                            messageTextBody.put("to", messageReceiverID);
                                            messageTextBody.put("messageID", messagePushID);
                                            messageTextBody.put("time", ServerValue.TIMESTAMP);


                                            Map messageBodyDetails = new HashMap();
                                            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
                                            messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);
                                            rootReference.updateChildren(messageBodyDetails);
                                            loadingbar.dismiss();

                                        }
                                    });
                                }
                            }

                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure( Exception e) {
                                loadingbar.dismiss();
                                Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double p = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                loadingbar.setMessage((int) p + "% Uploading...");
                            }
                        });

                    } else if (checker.equals("image")) {
                        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("images Folder");
                        final String messageSenderRef = "Messages/" + messageSenderId + "/" + messageReceiverID;
                        final String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderId;
                        DatabaseReference userMessageKeyRef = rootReference.child("Messages")
                                .child(messageSenderId).child(messageReceiverID).push();

                        final String messagePushID = userMessageKeyRef.getKey();
                        final StorageReference filepath = storageReference.child(messagePushID + "." + "jpg");
                        uploadTask = filepath.putFile(fileUri);
                        uploadTask.continueWithTask(new Continuation() {
                            @Override
                            public Object then( Task task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                return filepath.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete( Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    Uri downloaduri = task.getResult();
                                    myuri = downloaduri.toString();

                                    Map messageTextBody = new HashMap();
                                    messageTextBody.put("message", myuri);
                                    messageTextBody.put("name", fileUri.getLastPathSegment());
                                    messageTextBody.put("type", checker);
                                    messageTextBody.put("from", messageSenderId);
                                    messageTextBody.put("to", messageReceiverID);
                                    messageTextBody.put("messageID", messagePushID);
                                    messageTextBody.put("time", ServerValue.TIMESTAMP);


                                    Map messageBodyDetails = new HashMap();
                                    messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
                                    messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

                                    rootReference.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                        @Override
                                        public void onComplete( Task task) {
                                            if (task.isSuccessful()) {
                                                loadingbar.dismiss();
                                                Toast.makeText(ChatActivity.this, "Message Sent Successfully...", Toast.LENGTH_SHORT).show();
                                            } else {
                                                loadingbar.dismiss();
                                                Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                            }
                                            input_user_message.setText("");
                                        }
                                    });

                                } else {
                                    loadingbar.dismiss();
                                    Toast.makeText(ChatActivity.this, "No thing selected", Toast.LENGTH_SHORT).show();
                                }
                            }

                        });
                    }
                }
            }
        }
        else if (requestCode == CAMERA_REQ) {
            if (resultCode == RESULT_OK && data != null) {
                if (filePathImageCamera != null) {
                    loadingbar.setTitle("Send file");
                    loadingbar.setMessage("Loading");
                    loadingbar.setCanceledOnTouchOutside(false);
                    loadingbar.show();
                    fileUri = Uri.fromFile(filePathImageCamera);
                    StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Folder");
                    final String messageSenderRef = "Messages/" + messageSenderId + "/" + messageReceiverID;
                    final String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderId;
                    DatabaseReference userMessageKeyRef = rootReference.child("Messages")
                            .child(messageSenderId).child(messageReceiverID).push();
                    final String messagePushID = userMessageKeyRef.getKey();
                    final StorageReference filepath = storageReference.child(messagePushID + "." + "jpg");
                    uploadTask = filepath.putFile(fileUri);
                    uploadTask.continueWithTask(new Continuation() {
                        @Override
                        public Object then( Task task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return filepath.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete( Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloaduri = task.getResult();
                                myuri = downloaduri.toString();

                                Map messageTextBody = new HashMap();
                                messageTextBody.put("message", myuri);
                                messageTextBody.put("name", filePathImageCamera);
                                messageTextBody.put("type", checker);
                                messageTextBody.put("from", messageSenderId);
                                messageTextBody.put("to", messageReceiverID);
                                messageTextBody.put("messageID", messagePushID);
                                messageTextBody.put("time", ServerValue.TIMESTAMP);


                                Map messageBodyDetails = new HashMap();
                                messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
                                messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

                                rootReference.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                    @Override
                                    public void onComplete( Task task) {
                                        if (task.isSuccessful()) {
                                            loadingbar.dismiss();
                                            Toast.makeText(ChatActivity.this, "Message Sent Successfully...", Toast.LENGTH_SHORT).show();
                                        } else {
                                            loadingbar.dismiss();
                                            Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                        }
                                        input_user_message.setText("");
                                    }
                                });

                            } else {
                                loadingbar.dismiss();
                                Toast.makeText(ChatActivity.this, "Image saved to mobile", Toast.LENGTH_SHORT).show();
                            }
                        }

                    });

                }
            }
        } else if (requestCode == video_GALLERY_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                fileUri = data.getData();
                if (fileUri != null) {
                    loadingbar.setTitle("Send Video");
                    loadingbar.setMessage("Loading");
                    loadingbar.setCanceledOnTouchOutside(false);
                    loadingbar.show();
                    StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("videos folder");
                    final String messageSenderRef = "Messages/" + messageSenderId + "/" + messageReceiverID;
                    final String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderId;

                    DatabaseReference userMessageKeyRef = rootReference.child("Messages")
                            .child(messageSenderId).child(messageReceiverID).push();
                    final String messagePushID = userMessageKeyRef.getKey();
                    final StorageReference filepath = storageReference.child(messagePushID + "." + "mp4");
                    uploadTask = filepath.putFile(fileUri);
                    uploadTask.continueWithTask(new Continuation() {
                        @Override
                        public Object then( Task task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return filepath.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete( Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloaduri = task.getResult();
                                myuri = downloaduri.toString();
                                Map messageTextBody = new HashMap();
                                messageTextBody.put("message", myuri);
                                messageTextBody.put("name", fileUri.getLastPathSegment());
                                messageTextBody.put("type", checker);
                                messageTextBody.put("from", messageSenderId);
                                messageTextBody.put("to", messageReceiverID);
                                messageTextBody.put("messageID", messagePushID);
                                messageTextBody.put("time", ServerValue.TIMESTAMP);


                                Map messageBodyDetails = new HashMap();
                                messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
                                messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

                                rootReference.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                    @Override
                                    public void onComplete( Task task) {
                                        if (task.isSuccessful()) {
                                            loadingbar.dismiss();
                                            Toast.makeText(ChatActivity.this, "Message Sent Successfully...", Toast.LENGTH_SHORT).show();
                                        } else {
                                            loadingbar.dismiss();
                                            Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                        }
                                        input_user_message.setText("");
                                    }
                                });

                            } else {
                                loadingbar.dismiss();
                                Toast.makeText(ChatActivity.this, "No thing selected", Toast.LENGTH_SHORT).show();
                            }
                        }

                    });
                }
            }
        }
        else if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK) {
            fileUri = data.getData();
            if (fileUri != null) {
                Place place = PlacePicker.getPlace(this, data);
                if (place != null) {
                    loadingbar.setTitle("Send Location");
                    loadingbar.setMessage("Loading");
                    loadingbar.setCanceledOnTouchOutside(false);
                    loadingbar.show();
                    LatLng latLng = place.getLatLng();
                    final MapModel mapModel = new MapModel(latLng.latitude + "", latLng.longitude + "");
                    StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("map");
                    final String messageSenderRef = "Messages/" + messageSenderId + "/" + messageReceiverID;
                    final String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderId;
                    DatabaseReference userMessageKeyRef = rootReference.child("Messages")
                            .child(messageSenderId).child(messageReceiverID).push();
                    final String messagePushID = userMessageKeyRef.getKey();
                    final StorageReference filepath = storageReference.child(messagePushID + "." + "jpg");
                    uploadTask.continueWithTask(new Continuation() {
                        @Override
                        public Object then( Task task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return filepath.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete( Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloaduri = task.getResult();
                                myuri = downloaduri.toString();
                                Map messageTextBody = new HashMap();
                                //myuri = String.format("geo:%s,%s?z=17&q=%s,%s", latLng.latitude,latLng.c,latLng.latitude,latLng.longitude);
                                // myuri="https://maps.googleapis.com/maps/api/staticmap?center="+latitudeFinal+","+longitudeFinal+"&zoom=18&size=280x280&markers=color:red|"+latitudeFinal+","+longitudeFinal;
                                messageTextBody.put("message", myuri);
                                messageTextBody.put("name", mapModel);
                                messageTextBody.put("type", checker);
                                messageTextBody.put("from", messageSenderId);
                                messageTextBody.put("to", messageReceiverID);
                                messageTextBody.put("messageID", messagePushID);
                                messageTextBody.put("time", ServerValue.TIMESTAMP);


                                Map messageBodyDetails = new HashMap();
                                messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
                                messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

                                rootReference.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                    @Override
                                    public void onComplete( Task task) {
                                        if (task.isSuccessful()) {
                                            loadingbar.dismiss();
                                            Toast.makeText(ChatActivity.this, "Message Sent Successfully...", Toast.LENGTH_SHORT).show();
                                        } else {
                                            loadingbar.dismiss();
                                            Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                        }
                                        input_user_message.setText("");
                                    }
                                });

                            } else {
                                loadingbar.dismiss();
                                Toast.makeText(ChatActivity.this, "No thing selected", Toast.LENGTH_SHORT).show();
                            }
                        }

                    });
                }       //ChatModel chatModel = new ChatModel(userModel,Calendar.getInstance().getTime().getTime()+"",mapModel);
            }  //  mFirebaseDatabaseReference.child(CHAT_REFERENCE).push().setValue(chatModel);

        }
    }
}
