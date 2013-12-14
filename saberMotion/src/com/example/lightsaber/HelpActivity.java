package com.example.lightsaber;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;
import android.widget.TextView.BufferType;

public class HelpActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_layout);
		
		String instructions = "You are a Jedi in training." +
				"\nDefeat these droids by moving your device." +
				"\nSwing your device to swing the saber." +
				"\nHit the red bullets with the tip of your lightsaber.";
		SpannableString text = new SpannableString(instructions);
		
		TextView helpText = (TextView)findViewById(R.id.helpText);
		text.setSpan(new ForegroundColorSpan(Color.YELLOW), 0, instructions.length(), 0);
		helpText.setTextSize(31);
		helpText.setText(text, BufferType.SPANNABLE);
		
	}	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
	}	
}
