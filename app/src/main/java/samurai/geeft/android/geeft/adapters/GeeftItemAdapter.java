package samurai.geeft.android.geeft.adapters;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.Collections;
import java.util.List;

import samurai.geeft.android.geeft.R;
import samurai.geeft.android.geeft.models.Geeft;
import samurai.geeft.android.geeft.utilities.ImageControllerGenerator;

/**
 * Created by ugookeadu on 20/01/16.
 * adapter for GeeftListFragment Recyclerview
 */
public class GeeftItemAdapter extends RecyclerView.Adapter<GeeftItemAdapter.ViewHolder>{

    private final LayoutInflater inflater;


    //list containing the geefts and avoiding null pointer exception
    private List<Geeft> mGeeftList =
            Collections.emptyList();

    private int lastSize = 0;
    private Context mContext;

    private boolean pressed;

    //costructor
    public GeeftItemAdapter(Context context, List<Geeft> geeftList) {
        inflater = LayoutInflater.from(context);
        this.mGeeftList = geeftList;
        this.mContext = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView mTimeStampTextView;
        public TextView mUserLocationTextView;
        public TextView mUsernameTextView;
        public TextView mGeeftDescriptionTextView;
        public TextView mGeeftTitleTextView;

        public SimpleDraweeView mUserProfilePic;
        public SimpleDraweeView mGeeftImage;

        public ImageButton mPrenoteButton;
        public ImageButton mLocationButton;
        public ImageButton mShareButton;

        public CardView mContainer;


        public ViewHolder(View itemView) {
            super(itemView);
            mContainer = (CardView) itemView.findViewById(R.id.card_view);
            mGeeftTitleTextView = (TextView) itemView.findViewById(R.id.geeft_name);
            mGeeftDescriptionTextView = (TextView) itemView.findViewById(R.id.geeft_description);
            mUserLocationTextView = (TextView) itemView.findViewById(R.id.location);
            mUsernameTextView = (TextView) itemView.findViewById(R.id.geefter_name);
            mTimeStampTextView = (TextView) itemView.findViewById(R.id.timestamp);

            mUserProfilePic = (SimpleDraweeView) itemView.findViewById(R.id.geefter_profile_image);
            mGeeftImage = (SimpleDraweeView) itemView.findViewById(R.id.geeft_image);

            mPrenoteButton = (ImageButton) itemView.findViewById(R.id.geeft_like_reservation_button);
            mPrenoteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            mLocationButton = (ImageButton) itemView.findViewById(R.id.geeft_info_button);
            mShareButton = (ImageButton) itemView.findViewById(R.id.geeft_share_button);
        }

    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public GeeftItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // inflate the custom layout
        View mGeeftView = inflater.inflate(R.layout.geeft_list_item, parent, false);

        /** set the view's size, margins, paddings and layout parameters
         *
         */

        //Inflate a new view hierarchy from the specified xml resource.
        return new ViewHolder(mGeeftView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // - get element of the data model from list at this position
        Geeft item = mGeeftList.get(position);

        // - replace the contents of the view with that element
        holder.mUsernameTextView.setText(item.getUsername());
        holder.mGeeftDescriptionTextView.setText(item.getGeeftDescription());
        holder.mGeeftTitleTextView.setText(item.getGeeftTitle());
        holder.mTimeStampTextView.setText(item.getTimeStamp());
        holder.mUserLocationTextView.setText(item.getUserLocation());

        ImageControllerGenerator.generateSimpleDrawee(holder.mUserProfilePic,
                item.getUserProfilePic());
        ImageControllerGenerator.generateSimpleDrawee(holder.mGeeftImage,
                item.getGeeftImage());


        // Chcek for empty geeft title
        if (!TextUtils.isEmpty(item.getGeeftTitle()))
            // status is empty, remove from view
            holder.mGeeftTitleTextView.setVisibility(View.GONE);

        // Chcek for empty geeft description
        if (TextUtils.isEmpty(item.getGeeftDescription()))
            // description is empty, remove from view
            holder.mGeeftDescriptionTextView.setVisibility(View.GONE);

        if (TextUtils.isEmpty(item.getUserLocation())) {
            // location is empty, remove from view location txt and button
            holder.mUserLocationTextView.setVisibility(View.GONE);
            holder.mLocationButton.setVisibility(View.GONE);
        }
        setAnimation(holder.mContainer);
        holder.mPrenoteButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    pressed = !holder.mPrenoteButton.isPressed();
                    holder.mPrenoteButton.setPressed(pressed);
                    if (pressed)
                        holder.mPrenoteButton.setImageResource(R.drawable.checkbox_marked_circle_pressed);
                    else
                        holder.mPrenoteButton.setImageResource(R.drawable.checkbox_marked_circle);
                }
                return true;
            }

        });
    }

    @Override
    public int getItemCount() {
        return mGeeftList.size();
    }

    /**
     * Here is the key method to apply the animation
     */
    private void setAnimation(View viewToAnimate)
    {
        // If the bound view wasn't previously displayed on screen, it's animated
        if ((mGeeftList.size()-lastSize)>0)
        {
            Animation animation = AnimationUtils.loadAnimation(mContext, android.R.anim.slide_in_left);
            animation.setDuration(370);
            viewToAnimate.startAnimation(animation);
            lastSize++;
        }
    }
}