package ua.waserko.voxelgrove;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.opengl.GLSurfaceView;
import android.view.*;
import android.widget.FrameLayout;
import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Landscape touchscreen controller with a movable left stick, hold-to-mine and tap-to-place. */
public final class MainActivity extends Activity {
    World world; GameRenderer renderer; GLSurfaceView surface; Hud hud; File save;
    volatile String message="";
    public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);immersive();
        save=new File(getFilesDir(),"grove.vg");world=new World();if(!world.load(save))world.generate(2026);
        renderer=new GameRenderer(world,this);surface=new GLSurfaceView(this);surface.setEGLContextClientVersion(1);
        surface.setRenderer(renderer);surface.setPreserveEGLContextOnPause(true);
        FrameLayout root=new FrameLayout(this);root.addView(surface);hud=new Hud();root.addView(hud);setContentView(root);
    }
    void immersive(){getWindow().getDecorView().setSystemUiVisibility(5894);}
    public void onWindowFocusChanged(boolean f){super.onWindowFocusChanged(f);if(f)immersive();}
    void saveWorld(){try{world.save(save);message="Світ збережено";}catch(IOException e){message="Помилка збереження";}}
    protected void onPause(){renderer.paused=true;hud.menu=true;hud.panel=0;hud.reset();CountDownLatch done=new CountDownLatch(1);
        surface.queueEvent(()->{saveWorld();done.countDown();});try{done.await(1500,TimeUnit.MILLISECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}
        surface.onPause();super.onPause();}
    protected void onResume(){super.onResume();if(surface!=null)surface.onResume();}
    @Override public void onBackPressed(){if(hud.panel!=0){hud.closePanel();}
        else{renderer.paused=true;hud.menu=true;hud.reset();}hud.invalidate();}

    final class Hud extends View {
        final Paint p=new Paint(3);Canvas canvas;float scale=1,W,H;boolean menu=true,help=false;
        int panel=0,inventoryPage=0,moveId=-1,lookId=-1,mineId=-1,jumpId=-1;
        float stickX,stickY,anchorX=116,anchorY=343,lx,ly,touchX,touchY,lookTravel;
        long lookStart=0,mineStart=0,lastBreak=0;
        boolean crouch=false,sprint=false,lookMined=false;
        Bitmap tiles;
        Hud(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            try(InputStream in=getAssets().open("terrain.png")){tiles=BitmapFactory.decodeStream(in);}catch(IOException ignored){}
        }
        void reset(){moveId=lookId=mineId=jumpId=-1;stickX=stickY=0;lookStart=mineStart=0;renderer.side=renderer.forward=0;renderer.jump=false;renderer.sprint=false;renderer.crouch=false;}
        void fill(float x,float y,float w,float h,int c){p.setColor(c);p.setStyle(Paint.Style.FILL);canvas.drawRect(x,y,x+w,y+h,p);}
        void panelBox(float x,float y,float w,float h,int bg){fill(x+4,y+5,w,h,0x9b050c10);fill(x,y,w,h,bg);outline(x,y,w,h,0xff738080,2);}
        void outline(float x,float y,float w,float h,int c,float sw){p.setColor(c);p.setStrokeWidth(sw);p.setStyle(Paint.Style.STROKE);canvas.drawRect(x,y,x+w,y+h,p);p.setStyle(Paint.Style.FILL);}
        void text(String s,float x,float y,float size,int col){p.setColor(col);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));canvas.drawText(s,x,y,p);}
        void center(String s,float x,float y,float size,int col){p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));text(s,x-p.measureText(s)/2,y,size,col);}
        void button(float x,float y,float w,float h,String s,int bg,int fg){panelBox(x,y,w,h,bg);center(s,x+w/2,y+h/2+5,15,fg);}
        boolean in(float x,float y,float a,float b,float w,float h){return x>=a&&x<=a+w&&y>=b&&y<=b+h;}
        boolean near(float x,float y,float ax,float ay,float r){return Math.hypot(x-ax,y-ay)<r;}
        void round(float x,float y,float r,String label,boolean active){
            p.setColor(active?0xd8e8dbb7:0x94303f44);canvas.drawCircle(x,y,r+2,p);
            p.setColor(active?0xff667e63:0x93516164);canvas.drawCircle(x,y,r-2,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xbdeef4e9);canvas.drawCircle(x,y,r-2,p);p.setStyle(Paint.Style.FILL);
            center(label,x,y+7,20,Color.WHITE);
        }
        void imageTile(int type,float x,float y,float size){if(tiles==null||type<1||type>World.MAX_BLOCK)return;
            int tile=type==4?29:type==1?1:type;int cw=tiles.getWidth()/8,ch=tiles.getHeight()/4;
            int sx=(tile%8)*cw,sy=(tile/8)*ch;
            p.setColor(Color.WHITE);p.setFilterBitmap(false);p.setAlpha(255);
            canvas.drawBitmap(tiles,new Rect(sx,sy,sx+cw,sy+ch),new RectF(x,y,x+size,y+size),p);
        }
        @Override protected void onDraw(Canvas c){canvas=c;scale=Math.max(.1f,getHeight()/450f);W=getWidth()/scale;H=450;c.save();c.scale(scale,scale);
            if(menu)drawMenu();else{drawGame();if(panel!=0)drawPanel();else tickMine();}
            c.restore();postInvalidateDelayed(33);
        }
        void drawMenu(){fill(0,0,W,H,0xe412211c);
            for(int i=0;i<19;i++){int color=i%3==0?0xff233b2d:0xff192b25;fill(i*72-20,350+(i%3)*9,72,100,color);}
            text("WASERKO  •  WORLD BUILDER",47,48,16,0xffa4c995);
            text("VOXEL",44,129,67,0xffedf5e2);text("GROVE",44,197,67,0xffa8d77a);
            text("ВЛАСНИЙ 3D-СВІТ • БУДУЙ, ДОСЛІДЖУЙ, ВИЖИВАЙ",49,225,16,0xffb2c8ba);
            button(48,250,303,49,"ГРАТИ / ПРОДОВЖИТИ",0xff6da253,Color.WHITE);
            button(48,310,145,45,"НОВИЙ СВІТ",0xff405b55,Color.WHITE);
            button(206,310,145,45,world.creative?"ВИЖИВАННЯ":"ТВОРЧІСТЬ",0xff405b55,Color.WHITE);
            button(48,367,303,42,"ЯК ГРАТИ",0xff405b55,Color.WHITE);
            imageTile(1,W-330,102,145);imageTile(4,W-228,190,105);imageTile(14,W-375,257,78);
            if(help){panelBox(W/2-250,45,500,360,0xff172c2c);center("КЕРУВАННЯ",W/2,86,27,0xffbee991);
                center("ЛІВОРУЧ: джойстик руху · ⇧ швидкий біг",W/2,124,17,Color.WHITE);
                center("ПРАВОРУЧ: проведи пальцем для камери",W/2,159,17,Color.WHITE);
                center("Дотик до світу: поставити блок / ударити",W/2,194,17,Color.WHITE);
                center("Утримуй палець: видобувати блок",W/2,230,17,Color.WHITE);
                center("↑ — стрибок; ↓ — присідання",W/2,266,17,Color.WHITE);
                center("Сумка • крафт • вибір блока на панелі",W/2,301,16,Color.WHITE);
                button(W/2-105,332,210,48,"ЗРОЗУМІЛО",0xff6da253,Color.WHITE);
            }
        }
        void drawGame(){
            // Low-obstruction survival status / settings strip.
            fill(10,8,world.creative?192:292,64,0x87101a20);
            text("VOXEL GROVE 0.3",22,27,16,0xffc6eb97);
            if(world.creative)text("∞  ТВОРЧИЙ РЕЖИМ",22,51,14,Color.WHITE);
            else{for(int i=0;i<10;i++){text(i*2<world.health?"♥":"♡",22+i*13,52,17,i*2<world.health?0xffff6570:0xffb7b5b3);
                text(i*2<world.hunger?"◆":"◇",165+i*12,50,16,i*2<world.hunger?0xffe5b866:0xff8c9797);}}
            button(W-181,10,75,38,"СУМКА",0xb02f4446,Color.WHITE);
            button(W-96,10,79,38,"ПАУЗА",0xb02f4446,Color.WHITE);
            button(W-267,10,75,38,"КРАФТ",0xb02f4446,Color.WHITE);
            // Crosshair and mining indicator.
            p.setColor(0xf6ffffff);p.setStrokeWidth(2.3f);canvas.drawLine(W/2-8,225,W/2+8,225,p);canvas.drawLine(W/2,217,W/2,233,p);
            if((mineId!=-1||lookId!=-1&&lookStart>0&&!lookMined)&&mineStart>0){
                long elapsed=SystemClock.uptimeMillis()-mineStart;
                float progress=Math.min(1,elapsed/550f);
                fill(W/2-42,250,84,7,0xaa09161a);fill(W/2-40,252,80*progress,3,0xffb4e18c);
            }
            // Floating movement stick, easy to grab from the entire lower-left quarter.
            p.setColor(0x67404e58);canvas.drawCircle(anchorX,anchorY,68,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(0x9cb7c8cb);p.setStrokeWidth(2);canvas.drawCircle(anchorX,anchorY,66,p);p.setStyle(Paint.Style.FILL);
            p.setColor(0xacc3d0d2);canvas.drawCircle(anchorX+stickX*43,anchorY+stickY*43,26,p);
            round(94,242,25,"⇧",sprint);
            // Two right-hand physical action keys, world gestures also work.
            round(W-74,339,38,"↑",jumpId!=-1);
            round(W-74,248,30,"↓",crouch);
            round(W-165,344,35,"⛏",mineId!=-1);
            round(W-256,344,32,"▣",false);
            // Pixel-art hotbar with real atlas textures.
            float slot=49,start=W/2-slot*4;
            fill(start-7,392,8*slot+13,53,0xb010171b);
            for(int i=0;i<8;i++){
                float sx=start+i*slot;boolean chosen=world.hotbarIndex==i;
                fill(sx,397,45,43,chosen?0xffe9e8d0:0xc3334448);
                outline(sx+2,399,41,39,chosen?0xff6b7a62:0xff8e9fa0,2);
                int item=world.hotbar[i];imageTile(item,sx+8,404,29);
                if(!world.creative)text(""+world.bag[item],sx+31,436,11,Color.WHITE);
            }
            center(World.NAMES[world.selected],W/2,381,14,Color.WHITE);
            if(!message.isEmpty())center(message,W/2,88,14,0xffeff8e0);
        }
        void drawPanel(){fill(0,0,W,H,0xb3101c21);float x=W/2-318,y=26;panelBox(x,y,636,395,0xff243532);
            fill(x+10,y+10,616,42,0xff384c42);
            text(panel==1?"РЮКЗАК · ОБЕРИ БЛОК":"КРАФТ · НАТИСНИ РЕЦЕПТ",x+23,y+39,22,0xffe5f4d9);
            button(x+567,y+14,44,32,"X",0xff64736b,Color.WHITE);
            if(panel==1){int first=inventoryPage*16;
                for(int i=0;i<16;i++){int t=1+first+i;if(t>World.MAX_BLOCK)break;
                    float bx=x+21+(i%4)*152,by=y+62+(i/4)*69;
                    fill(bx,by,143,62,0xff40564b);outline(bx,by,143,62,0xff769082,1);
                    imageTile(t,bx+6,by+11,41);text(World.NAMES[t],bx+51,by+27,12,Color.WHITE);
                    text(world.creative?"НЕОБМЕЖЕНО":"К-СТЬ: "+world.bag[t],bx+51,by+48,10,0xffbcdda2);
                }
                button(x+23,y+354,130,30,"← НАЗАД",0xff516759,Color.WHITE);
                center("СТОРІНКА "+(inventoryPage+1)+" / 2",x+318,y+373,14,0xffe0eadf);
                button(x+482,y+354,130,30,"ДАЛІ →",0xff516759,Color.WHITE);
            }else{
                for(int i=0;i<World.RECIPES.length;i++){
                    int[] r=World.RECIPES[i];float bx=x+18+(i%2)*308,by=y+61+(i/2)*48;
                    fill(bx,by,301,44,0xff40564b);imageTile(r[0],bx+6,by+6,32);
                    String s=World.NAMES[r[0]]+" ×"+r[1];text(s,bx+44,by+18,14,Color.WHITE);
                    String req="";for(int k=2;k<r.length;k+=2)req+=World.NAMES[r[k]]+" "+r[k+1]+"  ";
                    if(req.length()>39)req=req.substring(0,39)+"…";text(req,bx+44,by+36,10,0xffcbd6bf);
                }
                center("ВИЖИВАННЯ: ПОТРІБНІ МАТЕРІАЛИ    •    ТВОРЧІСТЬ: БЕЗКОШТОВНО",x+318,y+375,13,0xffd9e4d3);
            }
        }
        void play(){menu=false;panel=0;renderer.paused=false;message="";reset();}
        void closePanel(){panel=0;renderer.paused=false;reset();}
        void mine(){surface.queueEvent(()->{if(world.edit(false))renderer.invalidateBlock(world.lastX,world.lastZ);});}
        void placeOrHit(){surface.queueEvent(()->{if(world.ray()!=null){if(world.edit(true))renderer.invalidateBlock(world.lastX,world.lastZ);}
            else if(world.hitMob())message="Істоту вражено";});}
        void tickMine(){long now=SystemClock.uptimeMillis();
            boolean holding=mineId!=-1 || (lookId!=-1&&lookStart>0&&lookTravel<14&&now-lookStart>330);
            if(holding){if(mineStart==0)mineStart=now;
                if(now-mineStart>=530 && now-lastBreak>=180){mine();lastBreak=now;mineStart=now;lookMined=true;}}
            else mineStart=0;
        }
        public boolean onTouchEvent(MotionEvent e){int action=e.getActionMasked(),idx=e.getActionIndex(),id=e.getPointerId(idx);
            float x=e.getX(idx)/scale,y=e.getY(idx)/scale;
            if(menu){if(action==MotionEvent.ACTION_DOWN){if(help)help=false;
                else if(in(x,y,48,250,303,49))play();
                else if(in(x,y,48,367,303,42))help=true;
                else if(in(x,y,206,310,145,45))surface.queueEvent(()->{world.creative=!world.creative;saveWorld();});
                else if(in(x,y,48,310,145,45))new AlertDialog.Builder(MainActivity.this).setTitle("Створити новий світ?")
                    .setMessage("Попередній світ буде замінено назавжди.").setNegativeButton("Скасувати",null)
                    .setPositiveButton("Створити",(d,w)->surface.queueEvent(()->{world.generate((int)System.currentTimeMillis());renderer.invalidateAll();saveWorld();runOnUiThread(()->play());})).show();
                invalidate();}return true;}
            if(panel!=0){if(action==MotionEvent.ACTION_DOWN){float ox=W/2-318,oy=26;
                if(in(x,y,ox+567,oy+14,44,32)||!in(x,y,ox,oy,636,395))closePanel();
                else if(panel==1){if(in(x,y,ox+23,oy+354,130,30))inventoryPage=0;
                    else if(in(x,y,ox+482,oy+354,130,30))inventoryPage=1;
                    else for(int i=0;i<16;i++){int t=1+inventoryPage*16+i;if(t>World.MAX_BLOCK)break;
                        float bx=ox+21+(i%4)*152,by=oy+62+(i/4)*69;
                        if(in(x,y,bx,by,143,62)){final int block=t;surface.queueEvent(()->world.choose(block));closePanel();break;}}
                }else{for(int i=0;i<World.RECIPES.length;i++){
                    float bx=ox+18+(i%2)*308,by=oy+61+(i/2)*48;
                    if(in(x,y,bx,by,301,44)){final int recipe=i;surface.queueEvent(()->message=world.craft(recipe)?"Предмет створено":"Бракує ресурсів");break;}
                }}invalidate();}return true;}
            if(action==MotionEvent.ACTION_CANCEL){reset();return true;}
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
                if(in(x,y,W-96,10,79,38)){menu=true;renderer.paused=true;reset();surface.queueEvent(()->saveWorld());}
                else if(in(x,y,W-181,10,75,38)){panel=1;inventoryPage=0;renderer.paused=true;reset();}
                else if(in(x,y,W-267,10,75,38)){panel=2;renderer.paused=true;reset();}
                else if(in(x,y,W/2-4*49,392,8*49,53)){
                    final int slot=Math.max(0,Math.min(7,(int)((x-(W/2-4*49))/49)));
                    surface.queueEvent(()->{world.hotbarIndex=slot;world.selected=world.hotbar[slot];});
                }else if(near(x,y,W-74,339,43)){jumpId=id;renderer.jump=true;}
                else if(near(x,y,W-74,248,38)){crouch=!crouch;renderer.crouch=crouch;}
                else if(near(x,y,W-165,344,40)){mineId=id;mineStart=SystemClock.uptimeMillis();}
                else if(near(x,y,W-256,344,38))placeOrHit();
                else if(near(x,y,94,242,31)){sprint=!sprint;renderer.sprint=sprint;}
                else if(x<W*.27f&&y>200){moveId=id;anchorX=x;anchorY=y;stick(x,y);}
                else {lookId=id;lx=touchX=x;ly=touchY=y;lookStart=SystemClock.uptimeMillis();lookTravel=0;lookMined=false;}
            }else if(action==MotionEvent.ACTION_MOVE){for(int i=0;i<e.getPointerCount();i++){
                int pid=e.getPointerId(i);float xx=e.getX(i)/scale,yy=e.getY(i)/scale;
                if(pid==moveId)stick(xx,yy);
                if(pid==lookId){float dx=xx-lx,dy=yy-ly;lx=xx;ly=yy;lookTravel+=Math.abs(dx)+Math.abs(dy);
                    if(dx!=0||dy!=0)surface.queueEvent(()->{world.yaw+=dx*.22f;world.pitch=Math.max(-85,Math.min(85,world.pitch-dy*.22f));});}
            }}else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){
                if(id==moveId){moveId=-1;stickX=stickY=0;renderer.side=renderer.forward=0;anchorX=116;anchorY=343;}
                if(id==jumpId){jumpId=-1;renderer.jump=false;}
                if(id==mineId){mineId=-1;mineStart=0;}
                if(id==lookId){if(!lookMined&&lookTravel<13&&SystemClock.uptimeMillis()-lookStart<300)placeOrHit();lookId=-1;lookStart=0;mineStart=0;}
            }return true;
        }
        void stick(float x,float y){stickX=(x-anchorX)/50;stickY=(y-anchorY)/50;float len=(float)Math.hypot(stickX,stickY);
            if(len>1){stickX/=len;stickY/=len;}renderer.side=stickX;renderer.forward=-stickY;}
    }
}
