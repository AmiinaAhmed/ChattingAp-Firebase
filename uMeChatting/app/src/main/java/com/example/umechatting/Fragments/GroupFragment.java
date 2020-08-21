package com.example.umechatting.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.umechatting.Model.Group;
import com.example.umechatting.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GroupFragment} interface
 * to handle interaction events.
 * Use the {@link GroupFragment} factory method to
 * create an instance of this fragment.
 */
public class GroupFragment extends Fragment {

    private View groupFragmentView;
    private RecyclerView group_list;



    private FirebaseAuth mAuth;
    String current_user_id;
    private DatabaseReference GroupRef;
    private DatabaseReference userDatabaseReference;

    public GroupFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        groupFragmentView = inflater.inflate(R.layout.fragment_group, container, false);


        group_list = groupFragmentView.findViewById(R.id.recycleListGroup);
        mAuth = FirebaseAuth.getInstance();
        current_user_id = mAuth.getCurrentUser().getUid();

        GroupRef = FirebaseDatabase.getInstance().getReference().child("users").child(current_user_id).child("Groups");
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users");


        return groupFragmentView;
    }
    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Group> recyclerOptions = new FirebaseRecyclerOptions.Builder<Group>()
                .setQuery(GroupRef, Group.class)
                .build();

        FirebaseRecyclerAdapter<Group,GroupFragment.ChatsVH> adapter = new FirebaseRecyclerAdapter<Group, GroupFragment.ChatsVH>(recyclerOptions) {
            @Override
            protected void onBindViewHolder(final GroupFragment.ChatsVH holder, int position, Group model) {

                GroupRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                          final String groupName = dataSnapshot.child("name").getValue().toString();

                            holder.groub_name.setText("groupname");

                          /*  holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // user active status validation
                                    if (dataSnapshot.exists()) {

                                        Intent chatIntent = new Intent(getContext(), GroupChatActivity.class);
                                        chatIntent.putExtra("visitUserId", userID);
                                        chatIntent.putExtra("userName", userName);
                                        startActivity(chatIntent);

                                    } else {
                                        GroupRef.child(userID).child("active_now")
                                                .setValue(ServerValue.TIMESTAMP).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                                chatIntent.putExtra("visitUserId", userID);
                                                chatIntent.putExtra("userName", userName);
                                                startActivity(chatIntent);
                                            }
                                        });



                                    }
                                }
                            });*/
                        }

                    }

                    @Override
                    public void onCancelled( DatabaseError databaseError) {

                    }
                });

            }


            @Override
            public GroupFragment.ChatsVH onCreateViewHolder(ViewGroup viewGroup, int viewType) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.all_single_group_chat, viewGroup, false);
                return new ChatsVH(view);
            }
        };

        group_list.setAdapter(adapter);
        adapter.startListening();
    }




    public static class ChatsVH extends RecyclerView.ViewHolder{

        TextView groub_name;
        CircleImageView group_photo;

        public ChatsVH(View itemView) {
            super(itemView);
            groub_name = itemView.findViewById(R.id.group_name);
            group_photo = itemView.findViewById(R.id.group_profile_img);

        }

    }





}
