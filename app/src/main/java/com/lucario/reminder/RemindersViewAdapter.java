package com.lucario.reminder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RemindersViewAdapter extends RecyclerView.Adapter<RemindersViewAdapter.ViewHolder> {
    private final Context context;
    private final click listener;
    private final ArrayList<Reminder> reminderArrayList;

    private final ArrayList<Integer> colorList = new ArrayList<>();
    RemindersViewAdapter(Context context, ArrayList<Reminder> reminderArrayList, click listener){
        this.context = context;
        this.listener = listener;
        this.reminderArrayList = reminderArrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.reminder_items_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.reminderTopic.setText(reminderArrayList.get(position).name);
        holder.circle.setBackground(generateCircularDrawable(context));
        holder.reminderView.setOnLongClickListener(e->{
            if (listener != null) {
                listener.onLongPress(position, colorList.get(position));
                return true;
            }
            return false;
        });
    }

    private Drawable generateCircularDrawable(Context context) {
        // Generate a random color for the border
        int borderAlpha = 255; // set alpha to fully opaque
        int borderRed = (int) (Math.random() * 256);
        int borderGreen = (int) (Math.random() * 256);
        int borderBlue = (int) (Math.random() * 256);
        int borderColor = Color.argb(borderAlpha, borderRed, borderGreen, borderBlue);
        colorList.add(borderColor);
        // Create a paint object for the border
        Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(borderColor);
        borderPaint.setStrokeWidth(2);

        // Create a paint object for the fill
        int fillAlpha = 128; // set alpha to half opaque
        int fillRed = (int) (borderRed * 0.8); // use a lighter version of the border color for the fill
        int fillGreen = (int) (borderGreen * 0.8);
        int fillBlue = (int) (borderBlue * 0.8);
        int fillColor = Color.argb(fillAlpha, fillRed, fillGreen, fillBlue);

        Paint fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(fillColor);

        // Create a new canvas
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw the circle with the border and fill paints
        canvas.drawCircle(50, 50, 48, borderPaint);
        canvas.drawCircle(50, 50, 48, fillPaint);

        // Convert the bitmap to a drawable and return it
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public interface click {
        void onLongPress(int position, int color);
    }

    @Override
    public int getItemCount() {
        return reminderArrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView reminderTopic;
        CardView reminderView;

        ImageView circle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
           reminderTopic = itemView.findViewById(R.id.reminderTopic);
           reminderView = itemView.findViewById(R.id.ritem);
           circle = itemView.findViewById(R.id.imageView);
        }
    }
}
