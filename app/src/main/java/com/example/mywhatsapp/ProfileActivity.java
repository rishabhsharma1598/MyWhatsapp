package com.example.mywhatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity
{
    private String receiverUserID,senderUserID,current_state;

    private CircleImageView userProfileImage;
     private TextView userProfileName,userProfileStatus;
    private Button sendMessageRequestButton,declineMessageRequestButton;

    private DatabaseReference userRef,chatRequestRef,contactsRef,notificationRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth=FirebaseAuth.getInstance();

        userRef= FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestRef= FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        contactsRef= FirebaseDatabase.getInstance().getReference().child("Contacts");
        notificationRef= FirebaseDatabase.getInstance().getReference().child("Notifications");
        
        receiverUserID=getIntent().getExtras().get("visit_user_id").toString();
        senderUserID=mAuth.getCurrentUser().getUid();

        userProfileImage=findViewById(R.id.visit_profile_image);
        userProfileName=findViewById(R.id.visit_user_name);
        userProfileStatus=findViewById(R.id.visit_profile_status);
        sendMessageRequestButton=findViewById(R.id.send_message_request_button);
        declineMessageRequestButton=findViewById(R.id.decline_message_request_button);
        current_state="new";

        RetrieveUserInfo();

    }

    private void RetrieveUserInfo()
    {
        userRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("image")))
                {
                    String userImage=dataSnapshot.child("image").getValue().toString();
                    String userName=dataSnapshot.child("name").getValue().toString();
                    String userStatus=dataSnapshot.child("status").getValue().toString();

                    Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);

                    ManageChatsRequest();
                }
                else
                {
                    String userName=dataSnapshot.child("name").getValue().toString();
                    String userStatus=dataSnapshot.child("status").getValue().toString();


                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);


                    ManageChatsRequest();
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }




    private void ManageChatsRequest()
    {
        chatRequestRef.child(senderUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (dataSnapshot.hasChild(receiverUserID))
                        {
                            String request_type=dataSnapshot.child(receiverUserID).child("request_type").getValue().toString();

                            if (request_type.equals("sent"))
                            {
                                current_state="request_sent";
                                sendMessageRequestButton.setText("Cancel Chat Request");
                            }
                            else if (request_type.equals("received"))
                            {
                                current_state="request_received";
                                sendMessageRequestButton.setText("Accept Chat Request");

                                declineMessageRequestButton.setVisibility(View.VISIBLE);
                                declineMessageRequestButton.setEnabled(true);
                                declineMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v)
                                    {
                                        CancelChatRequest();

                                    }
                                });
                            }
                        }
                        else
                        {
                            contactsRef.child(senderUserID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot)
                                        {
                                            if (dataSnapshot.hasChild(receiverUserID))
                                            {
                                                current_state="friends";
                                                sendMessageRequestButton.setText("Remove this Contact");
                                            }

                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                        }


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


        if (!senderUserID.equals(receiverUserID))
        {
           sendMessageRequestButton.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v)
               {
                   sendMessageRequestButton.setEnabled(false);

                   if (current_state.equals("new"))
                   {
                       SendChatRequest();
                   }
                   if (current_state.equals("request_sent"))
                   {
                       CancelChatRequest();
                   }
                   if (current_state.equals("request_received"))
                   {
                      AcceptChatRequest();
                   }
                   if (current_state.equals("friends"))
                   {
                       RemoveSpecificContact();
                   }



               }
           });
        }
        else
        {
            sendMessageRequestButton.setVisibility(View.INVISIBLE);
        }

    }



    private void RemoveSpecificContact()
    {
        contactsRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            contactsRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                sendMessageRequestButton.setEnabled(true);
                                                current_state="new";
                                                sendMessageRequestButton.setText("Send Message");
                                                declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                declineMessageRequestButton.setEnabled(false);
                                            }

                                        }
                                    });
                        }

                    }
                });

    }




    private void AcceptChatRequest()
    {
        contactsRef.child(senderUserID).child(receiverUserID)
                .child("Contacts").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            contactsRef.child(receiverUserID).child(senderUserID)
                                    .child("Contacts").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                chatRequestRef.child(senderUserID).child(receiverUserID)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task)
                                                            {
                                                                if (task.isSuccessful())
                                                                {
                                                                    chatRequestRef.child(receiverUserID).child(senderUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task)
                                                                                {
                                                                                    sendMessageRequestButton.setEnabled(true);
                                                                                    current_state="friends";

                                                                                    sendMessageRequestButton.setText("Remove this Contact");
                                                                                    declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                                                    declineMessageRequestButton.setEnabled(false);

                                                                                }
                                                                            });

                                                                }

                                                            }
                                                        });

                                            }

                                        }
                                    });

                        }

                    }
                });

    }




    private void CancelChatRequest()
    {
        chatRequestRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            chatRequestRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                sendMessageRequestButton.setEnabled(true);
                                                current_state="new";
                                                sendMessageRequestButton.setText("Send Message");
                                                declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                declineMessageRequestButton.setEnabled(false);
                                            }

                                        }
                                    });
                        }

                    }
                });
    }





    private void SendChatRequest()
    {
       chatRequestRef.child(senderUserID).child(receiverUserID)
               .child("request_type").setValue("sent")
               .addOnCompleteListener(new OnCompleteListener<Void>() {
                   @Override
                   public void onComplete(@NonNull Task<Void> task)
                   {
                       if (task.isSuccessful())
                       {
                           chatRequestRef.child(receiverUserID).child(senderUserID)
                                   .child("request_type").setValue("receive")
                                   .addOnCompleteListener(new OnCompleteListener<Void>() {
                                       @Override
                                       public void onComplete(@NonNull Task<Void> task)
                                       {
                                           if (task.isSuccessful())
                                           {

                                               HashMap<String, String> chatNotificationMap=new HashMap<>();
                                               chatNotificationMap.put("from", senderUserID);
                                               chatNotificationMap.put("type", "request");

                                               notificationRef.child(receiverUserID).push()
                                                       .setValue(chatNotificationMap)
                                                       .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                           @Override
                                                           public void onComplete(@NonNull Task<Void> task)
                                                           {
                                                               if (task.isSuccessful())
                                                               {
                                                                   sendMessageRequestButton.setEnabled(true);
                                                                   current_state = "request_sent";
                                                                   sendMessageRequestButton.setText("Cancel Chat Request");

                                                               }

                                                           }
                                                       });

                                           }

                                       }
                                   });
                       }

                   }
               });
    }
}