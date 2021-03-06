package com.codepath.apps.simpletweets.adapters;

import android.app.Activity;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.codepath.apps.simpletweets.R;
import com.codepath.apps.simpletweets.activities.DetailsActivity;
import com.codepath.apps.simpletweets.activities.TimelineActivity;
import com.codepath.apps.simpletweets.databinding.ItemTweetBinding;
import com.codepath.apps.simpletweets.fragments.ComposeDialogFragment;
import com.codepath.apps.simpletweets.models.Medium;
import com.codepath.apps.simpletweets.models.Tweet;
import com.codepath.apps.simpletweets.models.User;
import com.codepath.apps.simpletweets.models.ViewHolder;
import com.codepath.apps.simpletweets.others.HelperMethods;

import java.util.List;
/**
 * Created by Swati on 10/31/2016.
 */

public class ContactsAdapter extends RecyclerView.Adapter<ViewHolder>{

        // Store a member variable for the contacts
        private List<Tweet> contacts;
        // Store the context for easy access
        private Context context;
        // Define listener member variable
        private static OnItemClickListener listener;

    // Define the listener interface
        public interface OnItemClickListener {
            void onItemClick(View itemView, int position);
        }

        public static OnItemClickListener getListener() {
            return listener;
        }

        // Define the method that allows the parent activity or fragment to define the listener
        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        // Pass in the contact array into the constructor
        public ContactsAdapter(Context context, List<Tweet> contacts) {
            this.contacts = contacts;
            this.context = context;
        }

        // Easy access to the context object in the recyclerview
        private Context getContext() {
            return context;
        }

        // Usually involves inflating a layout from XML and returning the holder
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewHolder viewHolder;
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ItemTweetBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_tweet, parent, false);
            // distinguish view by viewType
            //View contactView = inflater.inflate(R.layout.item_tweet, parent, false);
            viewHolder = new ViewHolder(binding);
            return viewHolder;
        }

        // Involves populating data into the item through holder
        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            // get data item for position
            final Tweet tweet = contacts.get(position);
            final Tweet retweetedStatus = tweet.getRetweetedStatus();
            final Tweet targetTweet = retweetedStatus != null ? retweetedStatus : tweet;
            // Get binding
            final ItemTweetBinding binding = viewHolder.getBinding();

            // Reset views
            binding.ivMedia.setImageResource(0);
            binding.ivMedia.setVisibility(View.GONE);
            binding.vvMedia.setVisibility(View.GONE);
            binding.ivNotice.setVisibility(View.GONE);
            binding.tvNotice.setVisibility(View.GONE);

            // Add Tweet to the binding and
            binding.setTweet(targetTweet);

            // Set retweeted status notice
            if (retweetedStatus != null) {
                binding.tvNotice.setVisibility(View.VISIBLE);
                binding.ivNotice.setVisibility(View.VISIBLE);

                if (TimelineActivity.ACCOUNT != null &&
                    TimelineActivity.ACCOUNT.getName().equals(tweet.getUser().getName())) {
                    binding.tvNotice.setText("You Retweeted");
                } else {
                    binding.tvNotice.setText(tweet.getUser().getName() + " Retweeted");
                }
            }

            // Set Tweet content
            binding.tvBody.setText(targetTweet.getBody());

            // Search for @ and #
            HelperMethods.formatBody(getContext(), binding.tvBody);

            // Inflate media
            List<Medium> media = targetTweet.getMedia();
            int width = (int) context.getResources().getDimension(R.dimen.item_media_width);
            int height = (int) context.getResources().getDimension(R.dimen.item_media_height);
            if (media != null && media.size() > 0) {
                binding.ivMedia.setVisibility(View.VISIBLE);
                Glide.with(context)
                    .load(media.get(0).getMediaUrl())
                    .override(width, height)
                    .placeholder(R.drawable.ic_launcher_placeholder)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.ivMedia);

                // Get video
                final String videos = media.get(0).getVideos();
                if (videos != null) {
                    binding.vvMedia.setVisibility(View.VISIBLE);
                    // Play video
                    HelperMethods.playVideo(context, binding.vvMedia, null, videos);
                }
            } else {
                binding.tvBody.setPadding(0, 0, 0, (int) context.getResources().getDimension(R.dimen.item_content_margin));
            }

            // Execute binding immediately
            binding.executePendingBindings();

            // Set favorite listener
            binding.ivFavorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Switch favorite
                    HelperMethods.switchFavorite(tweet, binding.ivFavorite, binding.tvFavoriteCount);
                }
            });

            // Set retweet listener
            binding.ivRetweet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Switch retweet
                    HelperMethods.switchRetweet(tweet, binding.ivRetweet, binding.tvRetweetCount);
                }
            });

            // Set reply listener
            binding.ivReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ComposeDialogFragment composeDialogFragment = ComposeDialogFragment.newInstance(
                        DetailsActivity.REQUEST_REPLY, tweet);
                    composeDialogFragment.show(((Activity) getContext()).getFragmentManager(), "fragment_compose");
                }
            });
        }

        // Returns the total count of items in the list
        @Override
        public int getItemCount() {
            return contacts.size();
        }

        public static List<Tweet> getAll() {
            //return new ArrayList<Tweet>();
            // Query all saved Tweets
            List<Tweet> newTweets =
                new Select()
                .from(Tweet.class)
                .orderBy("tid DESC")    // large Tweet id == newer Tweet
                .execute();
            // Get media
            for (Tweet t : newTweets) {
                t.media = new Select()
                        .from(Medium.class)
                        .where("tweet = ?", t.getId())
                        .execute();
            }
            return newTweets;
        }

        public static void clearAll() {
            new Delete().from(User.class).execute();
            new Delete().from(Tweet.class).execute();
            new Delete().from(Medium.class).execute();
        }

        public void clear() {
            // Clear DB
            new Delete().from(Tweet.class).execute();
            // Clear Tweets list
            contacts.clear();
            // Notify adapter
            notifyDataSetChanged();
        }

        public void addAll(List<Tweet> list) {
            if (list != null) {
                // Save in DB
                for (Tweet tweet : list) {
                    tweet.save();
                }
                // Add to Tweets list
                contacts.addAll(list);
                // Notify adapter
                notifyDataSetChanged();
            }
        }

        public void add(Tweet newTweet) {
            add(contacts.size(), newTweet);
        }

        public void add(int pos, Tweet newTweet) {
            if (newTweet != null && pos >= 0 && pos <= contacts.size()) {
                // Save in DB
                newTweet.save();
                // Add to Tweets list
                contacts.add(pos, newTweet);
                // Notify adapter
                notifyDataSetChanged();
            }
        }

        public void update(int pos) {
            if (pos >= 0 && pos < contacts.size()) {
                // Save to DB
                contacts.get(pos).save();
                // Notify adapter
                notifyItemChanged(pos);
            }
        }

        public void remove(int pos) {
            if (pos >= 0 && pos < contacts.size()) {
                // Delete from DB
                contacts.get(pos).delete();
                // Delete related Medium
                new Delete().from(Medium.class).
                    where("tweet = ?", contacts.get(pos).getId())
                    .execute();
                // Remove from Tweets list
                contacts.remove(pos);
                // Notify adapter
                notifyDataSetChanged();
            }
        }
}
