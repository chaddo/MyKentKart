package com.mehmetakiftutuncu.mykentkart.adapters;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mehmetakiftutuncu.mykentkart.R;
import com.mehmetakiftutuncu.mykentkart.activities.KentKartDetailsActivity;
import com.mehmetakiftutuncu.mykentkart.models.KentKart;
import com.mehmetakiftutuncu.mykentkart.utilities.StringUtils;

import java.util.ArrayList;

public class KentKartAdapter extends RecyclerView.Adapter<KentKartAdapter.ViewHolder> {
    private ArrayList<KentKart> kentKarts;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_kentkart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        final KentKart kentKart = kentKarts != null ? kentKarts.get(position) : null;

        if (kentKart != null) {
            viewHolder.name.setText(kentKart.name);
            viewHolder.number.setText(kentKart.getFormattedNumber());
            viewHolder.nfc.setVisibility(StringUtils.isEmpty(kentKart.nfcId) ? View.GONE : View.VISIBLE);

            viewHolder.edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), KentKartDetailsActivity.class);
                    intent.setAction(KentKartDetailsActivity.EDIT_MODE);
                    intent.putExtra(KentKartDetailsActivity.KENT_KART_NAME, kentKart.name);
                    intent.putExtra(KentKartDetailsActivity.KENT_KART_NUMBER, kentKart.number);
                    intent.putExtra(KentKartDetailsActivity.KENT_KART_NFC_ID, kentKart.nfcId);
                    v.getContext().startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return kentKarts != null ? kentKarts.size() : 0;
    }

    public void setKentKarts(ArrayList<KentKart> kentKarts) {
        this.kentKarts = kentKarts;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public TextView number;
        public ImageView nfc;
        public ImageButton edit;

        public ViewHolder(View view) {
            super(view);

            this.name = (TextView) view.findViewById(R.id.textView_kentKart_name);
            this.number = (TextView) view.findViewById(R.id.textView_kentKart_number);
            this.nfc = (ImageView) view.findViewById(R.id.imageView_kentKart_nfcState);
            this.edit = (ImageButton) view.findViewById(R.id.imageButton_kentKart_edit);
        }
    }
}