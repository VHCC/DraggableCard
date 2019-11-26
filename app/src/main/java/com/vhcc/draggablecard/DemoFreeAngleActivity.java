package com.vhcc.draggablecard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vhcc.arclayoutlibs.Arc;
import com.vhcc.arclayoutlibs.ArcLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import static com.vhcc.arclayoutlibs.Arc.*;

public class DemoFreeAngleActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String KEY_DEMO = "demo";
    Toast toast = null;
    ArcLayout arcLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.arc_free_angle);
        arcLayout = (ArcLayout) findViewById(R.id.arc_layout);
        arcLayout.setArc(Arc.CENTER);

        for (int i = 0, size = arcLayout.getChildCount(); i < size; i++) {
            arcLayout.getChildAt(i).setOnClickListener(this);
        }

        TextView note = findViewById(R.id.note_text);
        note.setText("123");
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) note.getLayoutParams();
        switch (Arc.CENTER) {
            case TOP:
            case TOP_LEFT:
            case TOP_RIGHT:
                lp.gravity = Gravity.BOTTOM;
                break;
            case BOTTOM:
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                lp.gravity = Gravity.TOP;
                break;
            default:
                lp.gravity = Gravity.TOP;
        }

        ActionBar bar = getSupportActionBar();
        bar.setTitle("Advanced: Free angle");
        bar.setDisplayHomeAsUpEnabled(false);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v instanceof Button) {
            showToast((Button) v);
        }
    }

    private void showToast(Button btn) {
        if (toast != null) {
            toast.cancel();
        }

        String text = "Clicked: " + btn.getText();
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();

    }
}
