package com.example.lightsaber;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener 
{
	// Main view
	private FrameLayout mFrame;
	//there's definitely a better way to do this, but it ain't happening
	private Bitmap mBitmap, mBitmap1, mBitmap2, mBitmap3, mBitmap4;
	float speed = 0;
	float xSpeed = 0, ySpeed = 0;

    Boolean faceUp = false;
	//sensor stuff
	/* sensor data */
    SensorManager m_sensorManager;
    float []m_lastMagFields;
    float []m_lastAccels;
    float []m_lastGravity;
    private float[] m_rotationMatrix = new float[16];
    private float[] m_remappedR = new float[16];
    private float[] m_orientation = new float[4];
 
    /* fix random noise by averaging tilt values */
    final static int AVERAGE_BUFFER = 30;
    float []m_prevPitch = new float[AVERAGE_BUFFER];
    float m_lastPitch = 0.f;
    float m_lastYaw = 0.f;
    /* current index int m_prevEasts */
    int m_pitchIndex = 0;
 
    float []m_prevRoll = new float[AVERAGE_BUFFER];
    float m_lastRoll = 0.f;
    /* current index into m_prevTilts */
    int m_rollIndex = 0;
    Lightsaber saber;
    Background backView;
    //
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Set up user interface
		mFrame = (FrameLayout) findViewById(R.id.frame);
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inDither = false;
		opts.inPurgeable = true;
		opts.inInputShareable = true;
		opts.inTempStorage= new byte [32 * 1024];
		//yeah so decode resource for each direction
		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background, opts);
		mBitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.hands, opts);
		mBitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.leftswing, opts);
		mBitmap3 = BitmapFactory.decodeResource(getResources(), R.drawable.rightswing, opts);
		mBitmap4 = BitmapFactory.decodeResource(getResources(), R.drawable.downswing, opts);
		final Button leftButton = (Button) findViewById(R.id.left_button);
		final Button rightButton = (Button) findViewById(R.id.right_button);

		backView = new Background(
				getApplicationContext());
		saber = new Lightsaber(getApplicationContext());
		mFrame.addView(backView);
		mFrame.addView(saber);
		backView.start();
		saber.start();
		
		
		leftButton.setOnClickListener(new OnClickListener() 
		{
			//welp. scroll image left.

			public void onClick(View v) 
			{
				// TODO
				//backView.move(-2);
				//saber.move(-2);
			}
		});
		
		rightButton.setOnClickListener(new OnClickListener() {

			// Remove the last still-visible BubbleView from the screen
			// Manage RemoveButton

			public void onClick(View v) 
			{
				//backView.move(2);
				//saber.move(2);
				Toast.makeText(getApplicationContext(), "mFrame"+backView.mDisplayWidth +" "+mFrame.getWidth() + " "+saber.psuedoRotate + " " + saber.state, Toast.LENGTH_SHORT).show();
			}
		});
		
		//sensor stuff
        m_sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerListeners();
	}
	
	//sensorstuff

    private void registerListeners() {
    	m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_GAME);
        m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
        m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }
 
    private void unregisterListeners() {
        m_sensorManager.unregisterListener(this);
    }
 
    @Override
    public void onDestroy() {
        unregisterListeners();
        super.onDestroy();
    }
 
    @Override
    public void onPause() {
        unregisterListeners();
        super.onPause();
    }
 
    @Override
    public void onResume() {
        registerListeners();
        super.onResume();
    }
 
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
 
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accel(event);
            float z_value = event.values[2];
            if (z_value >= 0){
            	faceUp = true;
    			//Toast.makeText(getApplicationContext(), "faceup", Toast.LENGTH_SHORT).show();
    		    //face.setText("Face UP");
            }
            else{
            	faceUp = false;
    			//Toast.makeText(getApplicationContext(), "facedow", Toast.LENGTH_SHORT).show();
    		    //face.setText("Face Down");
            }
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mag(event);
        }
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            grav(event);
        }
        
    }

    private void accel(SensorEvent event) {
        if (m_lastAccels == null) {
            m_lastAccels = new float[3];
        }
 
        System.arraycopy(event.values, 0, m_lastAccels, 0, 3);
 
        /*if (m_lastMagFields != null) {
            computeOrientation();
        }*/
    }
 
    private void mag(SensorEvent event) {
        if (m_lastMagFields == null) {
            m_lastMagFields = new float[3];
        }
 
        System.arraycopy(event.values, 0, m_lastMagFields, 0, 3);
 
        if (m_lastAccels != null && m_lastGravity != null) {
            computeOrientation();
        }
    }
 
    private void grav(SensorEvent event) {
        if (m_lastGravity == null) {
            m_lastGravity = new float[3];
        }
 
        System.arraycopy(event.values, 0, m_lastGravity, 0, 3);
 
        /*if (m_lastMagFields != null) {
            computeOrientation();
        }*/
    }
    Filter [] m_filters = { new YawFilter(), new Filter(), new Filter() };
    
    private class Filter {
        static final int AVERAGE_BUFFER = 10;
        float []m_arr = new float[AVERAGE_BUFFER];
        float prev = 0;
        int m_idx = 0;
 
        public float append(float val) 
        {
            //look at the previous one, check if there is a dramatic shift
        	if(!faceUp)
        	{
        		val =  95 - (val - 95);
        		
        	}
            m_arr[m_idx] = val;
            prev = val;
            m_idx++;
            if (m_idx == AVERAGE_BUFFER)
                m_idx = 0;
            return avg();
        }
        public float avg() {
            float sum = 0;
            for (float x: m_arr)
                sum += x;
            return sum / AVERAGE_BUFFER;
        }
 
    }
 

    private class YawFilter extends Filter{
        static final int AVERAGE_BUFFER = 15;
        float []m_arr = new float[AVERAGE_BUFFER];
        Boolean set = false;
        float prevOrient = 0;
        float prev;
        int m_idx = 0;
        public float append(float val, float pitch) 
        {
            //look at the previous one, check if it's in discriminated territory
        	if(pitch < 115 && pitch > 70)
        	{
        		prevOrient = prev;
        		set = true;
        	}
        	
        	else
        	{	
        		if(!faceUp)
        		{
        			if(val > 0)
        				m_arr[m_idx] = val - 180;
        			else
        				m_arr[m_idx] = val + 180;
        				
        		}
        		else
        			m_arr[m_idx] = val;
            	m_idx++;
        	}
            if (m_idx == AVERAGE_BUFFER)
                m_idx = 0;
            return avg();
        }
        @Override
        public float avg() {
            float sum = 0;
            for (float x: m_arr)
                sum += x;
            prev = sum / AVERAGE_BUFFER;
            return sum / AVERAGE_BUFFER;
        }
 
    }
    private void computeOrientation() {
        if (SensorManager.getRotationMatrix(m_rotationMatrix, m_lastGravity, m_lastAccels, m_lastMagFields)) {
            SensorManager.getOrientation(m_rotationMatrix, m_orientation);
            
            /* 1 radian = 57.2957795 degrees */
            /* [0] : yaw, rotation around z axis
             * [1] : pitch, rotation around x axis
             * [2] : roll, rotation around y axis */
            float yaw = (float) (Math.toDegrees(m_orientation[0]));
            float pitch = (float) (Math.toDegrees(m_orientation[1]) + 180f);
            float roll = (float) (Math.toDegrees(m_orientation[2]) + 180f);

            float prevYaw = m_lastYaw;
            float prevPitch = m_lastPitch;
            float prevRoll = m_lastRoll;
            
            m_lastPitch = m_filters[1].append(pitch);
            m_lastYaw = ((YawFilter)m_filters[0]).append(yaw, m_lastPitch);
            m_lastRoll = m_filters[2].append(roll);
 

            
            //TextView rt = (TextView) findViewById(R.id.roll);
            TextView pt = (TextView) findViewById(R.id.pitch);
            TextView yt = (TextView) findViewById(R.id.yaw);
            yt.setText("azi z: " + m_lastYaw);
            //rt.setText("roll y: " + m_lastRoll);

            ySpeed = (m_lastPitch - prevPitch);
            pt.setText("pitch x: " + m_lastPitch);
            xSpeed = (m_lastYaw - prevYaw);
            //float ySpeed = m_lastPitch - prevPitch;
            backView.move(xSpeed * 5);
            if(xSpeed > 5 || xSpeed < -5)
            {
                if(saber.state == 3 || (m_lastPitch > 65 && m_lastPitch < 130))
                { 
                	saber.move(-xSpeed);
                }
                else
                	saber.move(-xSpeed * 25);
            }
            else
            	saber.move(-xSpeed);
            
            //float ySpeed = m_lastPitch - prevPitch;
            backView.moveUp(ySpeed * -3);
            if(ySpeed > 5 || ySpeed < -5)
            	saber.moveUp(-ySpeed * 25);
            else
            	saber.moveUp(-ySpeed);
        }
    }
    
    //end sensor stuff
	
	
	public class Background extends View
	{
		private static final int REFRESH_RATE = 40;
		float mX, mY, mSpeed, mUpSpeed;
		private final Paint mPainter = new Paint();
		float mDisplayWidth, mDisplayHeight;
		private ScheduledFuture<?> mMoverFuture;
		float mWidth, mHeight;
		public final Bitmap mBackground;
		public Background(Context context) 
		{
			super(context);
			// TODO Auto-generated constructor stub

			mDisplayWidth = 720;
			mDisplayHeight = 1080;
			
			int density = 150;
			mBackground = Bitmap.createScaledBitmap(mBitmap, 
					mBitmap.getWidth(), mBitmap.getHeight(),false);
			mWidth = mBitmap.getWidth();
			mHeight = mBitmap.getHeight();
			mSpeed = 0;
			mUpSpeed = 0;
			mX = 0;
			mY = 0;
			//System.out.println(mFrame.getWidth()+" "+ mDisplayWidth);
		}
		public void setWidth(int w, int h)
		{
			mDisplayWidth = w;
			mDisplayHeight = h;
		}
		private void start() 
		{
			//nothing fancy, just run. 
			ScheduledExecutorService executor = Executors
					.newScheduledThreadPool(1);

			mMoverFuture = executor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() 
				{

					update();
					postInvalidate();
				}
			}, 0, REFRESH_RATE, TimeUnit.MILLISECONDS);
		}
		private void update()
		{
			mX += mSpeed/2;
			if(mX > 0)
				mX = 0;

			if(mX + mWidth < mDisplayWidth)
				mX = mDisplayWidth-mWidth;
			
			mY += mUpSpeed/2;
			if(mY > 0)
				mY = 0;
			else if (mY + mHeight < mDisplayHeight)
				mY = mDisplayHeight - mHeight;
			mSpeed = 0;
			mUpSpeed = 0;
		}
		public void move(float speed)
		{
			mSpeed += speed;
		}
		public void moveUp(float speed)
		{
			mUpSpeed += speed;
		}
		@Override
		protected void onDraw(Canvas canvas) 
		{
			// TODO
			canvas.drawBitmap(this.mBackground, this.mX, this.mY, mPainter);
		}
	}
	public class PointXY
	{
		public float x, y;
		public PointXY(float x, float y)
		{
			this.x = x;
			this.y = y;
		}	
	}
	
	public class Lightsaber extends View 
	{
		private static final int REFRESH_RATE = 40;
		float mX, mY, centerX, centerY, pointX, pointY;
		private final Paint mPainter = new Paint();
		private int mDisplayWidth, mDisplayHeight;

		private Bitmap neutral, rightSlash, leftSlash, downSlash, currSlash;
		private float mSpeed, mUpSpeed;
		public int state = 0;
		private ScheduledFuture<?> mMoverFuture;
		public long mRotate, mDRotate, psuedoRotate;
		int shift = 12;
		int maxDegrees = 20;
		boolean swinging = false;
		public LinkedList<PointXY> hitBoxes;
		
		
		public Lightsaber(Context context) 
		{
			super(context);
			// TODO Auto-generated constructor stub
			mDisplayWidth = 720;
			mDisplayHeight = 1080;

			mPainter.setAntiAlias(true);
			neutral = Bitmap.createScaledBitmap(mBitmap1, mBitmap1.getWidth()*3/4, mBitmap1.getHeight()*3/4, false);
			rightSlash = Bitmap.createScaledBitmap(mBitmap3, mBitmap1.getWidth()*3/4, mBitmap1.getHeight()*3/4, false);
			leftSlash = Bitmap.createScaledBitmap(mBitmap2, mBitmap1.getWidth()*3/4, mBitmap1.getHeight()*3/4, false);
			downSlash = Bitmap.createScaledBitmap(mBitmap4, mBitmap1.getWidth()*3/4, mBitmap1.getHeight()*3/4, false);
			currSlash = neutral;
			pointX = mDisplayWidth/2;
			pointY = mDisplayHeight/2;
			centerX = pointX;
			centerY = pointY;
			mX = pointX - neutral.getWidth()/2;
			mY = pointY - neutral.getHeight()*1/4 - 60;
			//mNeutral = Bitmap.createScaledBitmap(mBitmap,
				//	mBitmap.getScaledWidth(2), mBitmap.getScaledHeight(2), false);
			//assign dem bitmaps
			mRotate = 0;
			psuedoRotate = 0;
			hitBoxes = new LinkedList<PointXY>();	
			mUpSpeed = 0;
		}
		
		private void start() 
		{
			//nothing fancy, just run. 
			ScheduledExecutorService executor = Executors
					.newScheduledThreadPool(1);

			mMoverFuture = executor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() 
				{
					update();
					postInvalidate();
				}
			}, 0, REFRESH_RATE, TimeUnit.MILLISECONDS);
		}
		private void move(float speed)
		{
			mSpeed += speed/15;
		}
		private void moveUp(float speed)
		{
			mUpSpeed += speed/15;
		}
		private void update()
		{
			//update the sword based current stuff.
			//add half of the current middle, or maybe a quarter? I'll figure it out.
			//if the difference is a certain speed, change the state. the state resets once the x is close to the real middle.
			mX += mSpeed;
			mY += mUpSpeed;
			pointX = mX + neutral.getWidth()/2;
			pointY = mY + neutral.getHeight()*1/4 + 60;
			//rotation stuff...
			mDRotate = (long) ((mSpeed - mRotate)/20);
			psuedoRotate += mDRotate;
			int rotScale = 1;
			int scale = 10;
			int yscale = 10;
			
			if(psuedoRotate > -shift && psuedoRotate < shift && mSpeed > -shift/2 && mSpeed < shift/2 && mUpSpeed < shift/2)
			{
				currSlash = neutral;
				state = 0;
				scale = 10;
				yscale = 10;
				swinging = false;
			}
			else if(psuedoRotate < -shift && !swinging)
			{
				currSlash = rightSlash;
				state = 1;
				swinging = true;
				scale = 10;
			}
			else if(psuedoRotate > shift && !swinging)
			{
				currSlash = leftSlash;
				state = 2;
				swinging = true;
				scale = 10;
			}

			else if(mUpSpeed/2 > shift && !swinging)
			{
				currSlash = downSlash;
				state = 3;
				swinging = true;
				mSpeed = mSpeed/20;
				scale = 5;
				yscale = 10;
			}
			if(mRotate > maxDegrees)
			{
				mRotate = maxDegrees;
				psuedoRotate = maxDegrees;
			}
			else if(mRotate < -maxDegrees)
			{
				mRotate = -maxDegrees;
				psuedoRotate = -maxDegrees;
			}
			
			//now lets slow down after a slash
			if(state == 1 && pointX > centerX + 300)
			{
				state = 0;
				currSlash = neutral;
				scale = 3;
				rotScale = 10;
			}
			else if(state == 2 && pointX < centerX - 300)
			{
				state = 0;
				currSlash = neutral;
				scale = 3;
				rotScale = 10;
			}
			mDRotate = mDRotate / rotScale;
			mRotate += mDRotate;
			//ease into the center
			if (pointX < centerX)
			{
				mSpeed += (centerX - pointX)/(2*scale);
				mX += (centerX - pointX)/scale;
				pointX += (centerX - pointX)/scale;
			}
			if (pointX > centerX)
			{
				mSpeed += (centerX - pointX)/(2*scale);
				mX += (centerX - pointX)/scale;
				pointX += (centerX - pointX)/scale;
			}
			if (pointY < centerY)
			{
				mUpSpeed += (centerY - pointY)/(2*yscale);
				mY += (centerY - pointY)/yscale;
				pointY += (centerY - pointY)/yscale;
			}
			if (pointY > centerY)
			{
				mUpSpeed += (centerY - pointY)/(2*yscale);
				mY += (centerY - pointY)/yscale;
				pointY += (centerY - pointY)/yscale;
			}

			if(state == 1 || state == 2 || state == 3)
			{
				hitBoxes.add(new PointXY(pointX, pointY));
				if(hitBoxes.size() > 10)
				{
					hitBoxes.poll();
				}
			}
		}
		public float slashSlope()
		{
			//when a sword going a certain speed passes the middle point, a slash occurs!
			//other wise, if it's slower, it'll be hitscan. I may not need to do a slash after all, depending on how this goes.
			return 0;
		}
		public float getPointX()
		{
			return pointX;
		}
		public float getPointY()
		{
			return pointY;
		}
		@Override
		protected void onDraw(Canvas canvas) {

			Log.i("Position", "onDraw:x:" + mX + " y:" + mY);

			canvas.save();
			//rotation, when we get there. it'll rotate slowly.
			if(state == 0)
				canvas.rotate(mRotate, pointX, pointY);
			else if(state == 1)
				canvas.rotate(mRotate + shift, pointX, pointY);
			else if(state == 2)
				canvas.rotate(mRotate - shift, pointX, pointY);
			canvas.drawBitmap(currSlash, mX, mY, mPainter);

			 Paint paint = new Paint();
			 paint.setColor(Color.GREEN);
			 paint.setStrokeWidth(2);
			 paint.setStyle(Paint.Style.STROKE);
			 
			canvas.drawCircle(pointX, pointY, 4, paint);
			//lets make loop of previous circles
			
			for(int i = 0; i < hitBoxes.size(); i++)
			{
				canvas.drawCircle(hitBoxes.get(i).x, hitBoxes.get(i).y, 3, paint);
			}
			
			canvas.restore();
		}

	}
	
	
}
