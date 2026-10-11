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

public final class MainActivity extends Activity {
    World world;GameRenderer renderer;GLSurfaceView surface;Hud hud;File save;
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
    @Override public void onBackPressed(){if(hud.panel!=0){hud.panel=0;renderer.paused=false;}
        else{renderer.paused=true;hud.menu=true;}hud.reset();hud.invalidate();}
    final class Hud extends View {
        Paint p=new Paint(3);Canvas canvas;float scale=1,W,H;boolean menu=true,help=false;int panel=0,inventoryPage=0;
        int moveId=-1,lookId=-1;float lx,ly,stickX,stickY;
        Hud(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void reset(){moveId=lookId=-1;stickX=stickY=0;renderer.side=renderer.forward=0;renderer.jump=false;}
        void rect(float x,float y,float w,float h,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);canvas.drawRoundRect(x,y,x+w,y+h,10,10,p);}
        void text(String s,float x,float y,float size,int col){p.setColor(col);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));canvas.drawText(s,x,y,p);}
        void center(String s,float x,float y,float size,int col){p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));text(s,x-p.measureText(s)/2,y,size,col);}
        void button(float x,float y,float w,float h,String s,int bg,int fg){rect(x,y,w,h,bg);center(s,x+w/2,y+h/2+5,13,fg);}
        boolean in(float x,float y,float a,float b,float w,float h){return x>=a&&x<=a+w&&y>=b&&y<=b+h;}
        void circle(float x,float y,float r,String s){p.setColor(0xb0153039);canvas.drawCircle(x,y,r,p);center(s,x,y+8,23,Color.WHITE);}
        protected void onDraw(Canvas c){canvas=c;scale=getHeight()/450f;W=getWidth()/scale;H=450;c.save();c.scale(scale,scale);
            if(menu)drawMenu();else{drawGame();if(panel!=0)drawPanel();}
            c.restore();postInvalidateDelayed(50);
        }
        void drawMenu(){rect(0,0,W,H,0xd2122732);
            text("WASERKO  /  BLOCK ADVENTURE",40,46,13,0xffb3dbba);
            text("VOXEL",40,112,54,Color.WHITE);text("GROVE",40,166,54,0xffb5e78a);
            text("Власна тривимірна пісочниця",44,202,17,Color.WHITE);
            button(44,232,290,48,"ГРАТИ / ПРОДОВЖИТИ",0xffb5e78a,0xff182d2c);
            button(44,288,139,44,"Новий світ",0x8843686d,Color.WHITE);
            button(194,288,139,44,world.creative?"Виживання":"Творчість",0x8843686d,Color.WHITE);
            button(44,344,289,36,"Керування",0x8843686d,Color.WHITE);
            text("Альфа 0.2  •  текстури  •  істоти  •  крафт",44,407,12,0xffc7d8d8);
            if(help){rect(W/2-230,55,460,344,0xff162f38);center("КЕРУВАННЯ",W/2,94,23,0xffb5e78a);
                center("Джойстик зліва — рух",W/2,130,16,Color.WHITE);
                center("Палець справа — камера",W/2,161,16,Color.WHITE);
                center("− ламати / + ставити / ↑ стрибати",W/2,193,15,Color.WHITE);
                center("⚔ — удар / Їжа — поїсти",W/2,225,15,Color.WHITE);
                center("Сумка — інвентар / Крафт — рецепти",W/2,257,15,Color.WHITE);
                center("Прогрес зберігається автоматично",W/2,288,14,0xffb3cad0);
                button(W/2-84,324,168,42,"ЗРОЗУМІЛО",0xffb5e78a,0xff182d2c);}
        }
        void drawGame(){
            rect(14,13,209,world.creative?40:65,0x9c142b32);
            text("VOXEL GROVE 0.2",27,31,14,0xffb5e78a);
            if(world.creative)text("ТВОРЧИЙ РЕЖИМ",27,47,10,0xffc3d2d4);
            else{ text("Здоров'я "+(int)world.health+"/20",27,49,12,0xffff9995);
                  text("Ситість "+(int)world.hunger+"/20",127,49,12,0xffffd591);
                  text("СВІТ ОФЛАЙН",27,67,9,0xffc4d5d3);}
            button(W-254,16,80,39,"Сумка",0xaa152d35,Color.WHITE);
            button(W-167,16,75,39,"Крафт",0xaa152d35,Color.WHITE);
            button(W-85,16,70,39,"Пауза",0xaa152d35,Color.WHITE);
            p.setColor(0xeaffffff);p.setStrokeWidth(2);canvas.drawLine(W/2-8,H/2,W/2+8,H/2,p);canvas.drawLine(W/2,H/2-8,W/2,H/2+8,p);
            p.setColor(0x6020373f);canvas.drawCircle(100,333,68,p);p.setColor(0x805a8a88);canvas.drawCircle(100,333,47,p);
            p.setColor(0xe3c6e7cf);canvas.drawCircle(100+stickX*40,333+stickY*40,24,p);
            circle(W-238,320,30,"⚔");circle(W-156,320,30,"−");circle(W-74,320,30,"+");
            circle(W-74,239,27,"↑");circle(W-154,239,27,"Їжа");
            float start=W/2-184;rect(start-8,386,376,56,0xb3152b33);
            for(int i=0;i<8;i++){int t=world.hotbar[i];float left=start+i*46;
                rect(left,392,40,42,world.hotbarIndex==i?0xffddf7b7:0xff385059);
                float[] rgb=World.COLORS[t];int color=Color.rgb((int)(rgb[0]*255),(int)(rgb[1]*255),(int)(rgb[2]*255));
                rect(left+7,398,26,26,color);text(""+(i+1),left+3,429,9,0xff182e31);
                if(!world.creative)text(""+world.bag[t],left+23,430,9,Color.WHITE);
            }
            center(World.NAMES[world.selected],W/2,376,13,Color.WHITE);
            if(!message.isEmpty())center(message,W/2,66,12,0xffecfbd9);
        }
        void drawPanel(){rect(0,0,W,H,0xb5111e23);float x=W/2-320,y=32;rect(x,y,640,384,0xff1a343d);
            text(panel==1?"РЮКЗАК  •  ОБЕРИ БЛОК":"КРАФТ  •  РЕЦЕПТИ",x+24,y+33,22,0xffb5e78a);
            button(x+557,y+10,60,35,"X",0xff47616a,Color.WHITE);
            if(panel==1){int first=inventoryPage*16;
                for(int i=0;i<16;i++){int t=1+first+i;if(t>World.MAX_BLOCK)break;
                    float bx=x+21+(i%4)*154,by=y+52+(i/4)*74;
                    rect(bx,by,143,63,0xff31505a);
                    text(World.NAMES[t],bx+7,by+23,Math.min(13,World.NAMES[t].length()>15?10:12),Color.WHITE);
                    text(world.creative?"∞":"Є: "+world.bag[t],bx+7,by+47,13,0xffc6e9ac);
                }
                button(x+18,y+348,139,30,"< Назад",0xff39545d,Color.WHITE);
                center("Сторінка "+(inventoryPage+1)+" / 2",x+318,y+369,13,0xffc3d9dc);
                button(x+492,y+348,130,30,"Далі >",0xff39545d,Color.WHITE);
            }else{
                for(int i=0;i<World.RECIPES.length;i++){
                    int[] r=World.RECIPES[i];float bx=x+21+(i%2)*310,by=y+50+(i/2)*48;
                    rect(bx,by,300,42,0xff2b4b54);
                    String s=World.NAMES[r[0]]+" ×"+r[1];text(s,bx+8,by+17,14,0xffffffff);
                    String req="";for(int k=2;k<r.length;k+=2)req+=World.NAMES[r[k]]+" "+r[k+1]+"  ";
                    if(req.length()>43)req=req.substring(0,42)+"…";
                    text(req,bx+8,by+34,10,0xffc1d4d8);
                }
                button(x+200,y+348,238,30,world.creative?"Безкоштовно в творчості":"Потрібні ресурси",0xff39545d,Color.WHITE);
            }
        }
        void play(){menu=false;panel=0;renderer.paused=false;message="";reset();}
        void closePanel(){panel=0;renderer.paused=false;reset();}
        public boolean onTouchEvent(MotionEvent e){int action=e.getActionMasked(),idx=e.getActionIndex(),id=e.getPointerId(idx);
            float x=e.getX(idx)/scale,y=e.getY(idx)/scale;
            if(menu){if(action==MotionEvent.ACTION_DOWN){if(help){help=false;}else if(in(x,y,44,232,290,48))play();
                else if(in(x,y,44,344,289,36))help=true;
                else if(in(x,y,194,288,139,44)){surface.queueEvent(()->{world.creative=!world.creative;saveWorld();});}
                else if(in(x,y,44,288,139,44)){
                    new AlertDialog.Builder(MainActivity.this).setTitle("Новий світ?").setMessage("Поточний світ буде замінено. Дію не можна скасувати.")
                      .setNegativeButton("Скасувати",null).setPositiveButton("Створити",(d,w)->surface.queueEvent(()->{
                         world.generate((int)System.currentTimeMillis());renderer.invalidateAll();saveWorld();runOnUiThread(()->play());})).show();
                }}invalidate();return true;}
            if(panel!=0){if(action==MotionEvent.ACTION_DOWN){float ox=W/2-320,oy=32;
                if(in(x,y,ox+557,oy+10,60,35)||!in(x,y,ox,oy,640,384)){closePanel();}
                else if(panel==1){
                    if(in(x,y,ox+18,oy+348,139,30))inventoryPage=0;
                    else if(in(x,y,ox+492,oy+348,130,30))inventoryPage=1;
                    else for(int i=0;i<16;i++){int t=1+inventoryPage*16+i;
                        if(t>World.MAX_BLOCK)break;float bx=ox+21+(i%4)*154,by=oy+52+(i/4)*74;
                        if(in(x,y,bx,by,143,63)){final int block=t;surface.queueEvent(()->world.choose(block));closePanel();break;}}
                }else if(panel==2){for(int i=0;i<World.RECIPES.length;i++){
                        float bx=ox+21+(i%2)*310,by=oy+50+(i/2)*48;
                        if(in(x,y,bx,by,300,42)){final int recipe=i;surface.queueEvent(()->message=world.craft(recipe)?"Предмет створено":"Бракує ресурсів");break;}
                    }}
                invalidate();}return true;}
            if(action==MotionEvent.ACTION_CANCEL){reset();return true;}
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
                if(in(x,y,W-85,16,70,39)){menu=true;renderer.paused=true;reset();surface.queueEvent(()->saveWorld());}
                else if(in(x,y,W-254,16,80,39)){panel=1;inventoryPage=0;renderer.paused=true;reset();}
                else if(in(x,y,W-167,16,75,39)){panel=2;renderer.paused=true;reset();}
                else if(y>=386&&in(x,y,W/2-184,386,368,60)){int slot=Math.max(0,Math.min(7,(int)((x-(W/2-184))/46)));
                    surface.queueEvent(()->{world.hotbarIndex=slot;world.selected=world.hotbar[slot];});}
                else if(Math.hypot(x-(W-74),y-239)<36)renderer.jump=true;
                else if(Math.hypot(x-(W-154),y-239)<36)surface.queueEvent(()->message=world.eat()?"Ситість відновлено":"Немає їжі");
                else if(Math.hypot(x-(W-238),y-320)<38)surface.queueEvent(()->message=world.hitMob()?"Істоту вражено":"Занадто далеко");
                else if(Math.hypot(x-(W-156),y-320)<38)surface.queueEvent(()->{if(world.edit(false))renderer.invalidateBlock(world.lastX,world.lastZ);});
                else if(Math.hypot(x-(W-74),y-320)<38)surface.queueEvent(()->{if(world.edit(true))renderer.invalidateBlock(world.lastX,world.lastZ);else message="Недоступне місце або блок";});
                else if(x<200&&y>230){moveId=id;stick(x,y);}
                else if(x>W*.32f){lookId=id;lx=x;ly=y;}
            }else if(action==MotionEvent.ACTION_MOVE){for(int i=0;i<e.getPointerCount();i++){
                int pid=e.getPointerId(i);float xx=e.getX(i)/scale,yy=e.getY(i)/scale;
                if(pid==moveId)stick(xx,yy);
                if(pid==lookId){float dx=xx-lx,dy=yy-ly;lx=xx;ly=yy;surface.queueEvent(()->{
                    world.yaw+=dx*.25f;world.pitch=Math.max(-85,Math.min(85,world.pitch-dy*.25f));});}
            }}else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){
                if(id==moveId){moveId=-1;stickX=stickY=0;renderer.side=renderer.forward=0;}if(id==lookId)lookId=-1;
            }return true;
        }
        void stick(float x,float y){stickX=(x-100)/50;stickY=(y-333)/50;float len=(float)Math.hypot(stickX,stickY);
            if(len>1){stickX/=len;stickY/=len;}renderer.side=stickX;renderer.forward=-stickY;}
    }
}
