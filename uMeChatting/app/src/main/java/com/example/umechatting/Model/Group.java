package com.example.umechatting.Model;

import java.util.ArrayList;

/**
 * @author Amina A. Abounawara
 * @date 8/2/2019
 */
public class Group {
    public String name;
    public ArrayList<Friends> listFriend;
    public Group(){
        listFriend = new ArrayList<Friends>();
    }
}
