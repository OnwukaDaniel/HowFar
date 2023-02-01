package com.azur.howfar.user.vip;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.azur.howfar.R;
import com.azur.howfar.activity.BaseActivity;
import com.azur.howfar.adapter.DotAdapter;
import com.azur.howfar.databinding.ActivityVipPlanBinding;

public class VipPlanActivity extends BaseActivity {
    ActivityVipPlanBinding binding;
    VipImagesAdapter vipImagesAdapter = new VipImagesAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_vip_plan);


        VipPlanAdapter vipPlanAdapter = new VipPlanAdapter();

        binding.rvPlan.setAdapter(vipPlanAdapter);


        setVIpSlider();


    }

    private void setVIpSlider() {
        binding.rvBanner.setAdapter(vipImagesAdapter);

        DotAdapter dotAdapter = new DotAdapter(vipImagesAdapter.getItemCount(), R.color.pink);
        binding.rvDots.setAdapter(dotAdapter);
        binding.rvBanner.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager myLayoutManager = (LinearLayoutManager) binding.rvBanner.getLayoutManager();
                int scrollPosition = myLayoutManager.findFirstVisibleItemPosition();
                dotAdapter.changeDot(scrollPosition);
            }
        });

    }


}