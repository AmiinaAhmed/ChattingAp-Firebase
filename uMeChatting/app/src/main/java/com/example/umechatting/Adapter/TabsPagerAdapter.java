package com.example.umechatting.Adapter;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.umechatting.Fragments.ChatsFragment;
import com.example.umechatting.Fragments.GroupFragment;
import com.example.umechatting.Fragments.RequestsFragment;

public class TabsPagerAdapter extends FragmentPagerAdapter {


    public TabsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                ChatsFragment chatsFragment = new ChatsFragment();
                return chatsFragment;
            case 1:
                GroupFragment groupFragment = new GroupFragment();
                return  groupFragment;
            case 2:
                RequestsFragment requestsFragment = new RequestsFragment();
                return requestsFragment;
            default:
                return null;

        }
    }

    @Override
    public int getCount() {
        return 3; // 3 is total fragment number (e.x- Chats, Groups, Requests)
    }


    @Override
    public CharSequence getPageTitle(int position) {
        switch (position){
            case 0:
                return "CHATS"; // ChatsFragment
            case 1:
                return "GROUPS"; //Group Frgment
            case 2:
                return "REQUESTS"; // RequestsFragment
            default:
                return null;
        }
        //return super.getPageTitle(position);
    }
}
