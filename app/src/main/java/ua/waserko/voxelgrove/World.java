package ua.waserko.voxelgrove;

import java.io.*;
import java.util.*;

/** Deterministic voxel terrain, lightweight survival and backwards-compatible persistence. */
public final class World {
    public static final int N=48,H=28,MAX_BLOCK=27,MAX_ITEM=34;
    public static final String[] NAMES={"", "Трава", "Земля", "Камінь", "Колода", "Листя", "Пісок", "Цегла", "Кристал",
        "Вода", "Вугілляна руда", "Залізна руда", "Мідна руда", "Золота руда", "Алмазна руда", "Гравій", "Дошки", "Скло", "Пісковик", "Сніг", "Кактус", "Верстак", "Піч", "Корінна порода", "Глина", "Обсидіан", "Мох", "Сапфірова руда", "Яблуко", "М'ясо", "Смажене м'ясо", "Залізний злиток", "Мідний злиток", "Золотий злиток", "Палиця"};
    public static final float[][] COLORS={
        {0,0,0},{.40f,.68f,.30f},{.53f,.36f,.22f},{.54f,.60f,.64f},{.50f,.31f,.16f},{.24f,.53f,.29f},
        {.86f,.77f,.51f},{.69f,.35f,.28f},{.29f,.80f,.80f},{.18f,.46f,.76f},{.24f,.27f,.32f},
        {.69f,.49f,.37f},{.66f,.42f,.21f},{.82f,.67f,.20f},{.27f,.67f,.82f},{.61f,.56f,.50f},
        {.68f,.47f,.27f},{.68f,.82f,.86f},{.82f,.70f,.45f},{.88f,.94f,.96f},{.28f,.58f,.27f},
        {.69f,.50f,.34f},{.46f,.46f,.50f},{.19f,.20f,.23f},{.55f,.63f,.70f},{.20f,.16f,.31f},
        {.31f,.47f,.28f},{.28f,.52f,.91f}
    };
    public byte[] blocks=new byte[N*H*N];
    public float x=24.5f,y=14,z=24.5f,yaw=0,pitch=-12,vy,health=20,hunger=20,dayTime=.25f;
    public boolean grounded,creative=true, crouching=false, sprinting=false;
    public int selected=1,seed=2026,hotbarIndex=0;
    public final int[] bag=new int[MAX_ITEM+1];
    public final int[] hotbar={1,2,3,4,6,16,17,21};
    public final ArrayList<Mob> mobs=new ArrayList<>();
    public int lastX=-1,lastZ=-1;
    public float cameraHeight(){return crouching?1.12f:1.55f;}
    private float lifeClock=0,mobClock=0;
    private final Random gameRandom=new Random(2026);
    public static final class Mob {
        public int type; // 0 = grazing sheep, 1 = boar, 2 = shadow creature
        public float x,y,z,phase;
        public Mob(int t,float xx,float yy,float zz,float p){type=t;x=xx;y=yy;z=zz;phase=p;}
    }
    // Crafted amount, then ingredient pairs. Ingredients refer to both blocks and bag-only items.
    public static final int[][] RECIPES={
        {16,4,4,1}, {34,4,16,2}, {21,1,16,4}, {22,1,3,8}, {7,4,3,4},
        {17,4,6,4}, {30,1,29,1,10,1}, {31,1,11,1,10,1}, {32,1,12,1,10,1},
        {33,1,13,1,10,1}, {18,4,6,4}, {8,1,27,2,17,1}
    };
    int index(int a,int b,int c){return (a*H+b)*N+c;}
    public int get(int a,int b,int c){if(a<0||a>=N||b<0||b>=H||c<0||c>=N)return 0;return blocks[index(a,b,c)]&255;}
    public void set(int a,int b,int c,int t){if(a>=0&&a<N&&b>=0&&b<H&&c>=0&&c<N)blocks[index(a,b,c)]=(byte)t;}
    static boolean passable(int t){return t==0||t==9;}
    public void generate(int s){
        seed=s;blocks=new byte[N*H*N];Random r=new Random(s);mobs.clear();Arrays.fill(bag,0);creative=true;
        for(int a=0;a<N;a++)for(int c=0;c<N;c++){
            double n=Math.sin((a+s*.04)*.145)*2.4+Math.cos((c-s*.03)*.164)*2.1+Math.sin((a+c)*.093)*1.4;
            int h=6+(int)Math.round(n);h=Math.max(3,Math.min(13,h));
            double climate=Math.sin((a+s*.012)*.09)+Math.cos((c-s*.03)*.105);
            int biome=climate>.83?1:climate<-.78?2:0; // desert, snow, woodland
            for(int b=0;b<=h;b++){
                int t=b==0?23:(b==h?(biome==1?6:biome==2?19:1):b>=h-3?(biome==1?6:2):3);
                if(b>1&&b<h-1&&b<h-3&&Math.sin(a*.52+b*.67+s*.004)*Math.cos(c*.51-b*.31)>.76)t=0;
                if(t==3){int ore=r.nextInt(240);if(b<5&&ore==0)t=14;else if(b<9&&ore<3)t=13;else if(ore<7)t=11;else if(ore<15)t=12;else if(ore<25)t=10;else if(b<4&&ore==28)t=27;}
                set(a,b,c,t);
            }
            if(h<5)for(int b=h+1;b<=5;b++)set(a,b,c,9);
        }
        for(int i=0;i<73;i++){
            int a=3+r.nextInt(N-6),c=3+r.nextInt(N-6);if(Math.abs(a-24)<5&&Math.abs(c-24)<5)continue;
            int h=groundTop(a,c),surface=get(a,h-1,c);
            if(surface==6&&r.nextBoolean()){
                for(int b=0;b<2+r.nextInt(2)&&h+b<H-1;b++)set(a,h+b,c,20);
            }else if(surface==1){
                int trunk=3+r.nextInt(3);for(int b=0;b<trunk;b++)set(a,h+b,c,4);
                for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=trunk-2;dy<=trunk+1;dy++)
                    if(Math.abs(dx)+Math.abs(dz)+(dy==trunk+1?1:0)<4&&get(a+dx,h+dy,c+dz)==0)set(a+dx,h+dy,c+dz,5);
            }
        }
        makeCabin(10,11);makeRuin(36,36);
        x=24.5f;z=24.5f;y=groundTop(24,24)+.05f;yaw=0;pitch=-12;vy=0;health=hunger=20;dayTime=.25f;selected=1;hotbarIndex=0;
        // Clear any foliage obstructing spawn.
        for(int i=0;i<3;i++)for(int a=23;a<=25;a++)for(int c=23;c<=25;c++)set(a,(int)y+i,c,0);
        for(int i=0;i<12;i++){
            int a=4+r.nextInt(N-8),c=4+r.nextInt(N-8);
            int b=groundTop(a,c);if(get(a,b-1,c)==9||Math.abs(a-24)+Math.abs(c-24)<8)continue;
            mobs.add(new Mob(i%5==0?2:i%2, a+.5f,b,c+.5f,r.nextFloat()*6.28f));
        }
        lastX=-1;lastZ=-1;crouching=false;sprinting=false;
    }
    private int groundTop(int a,int c){for(int b=H-2;b>=0;b--){int t=get(a,b,c);if(t!=0&&t!=9&&t!=4&&t!=5&&t!=20)return b+1;}return 2;}
    int top(int a,int c){return groundTop(a,c);}
    void makeCabin(int a,int c){int floor=groundTop(a,c);if(floor+5>=H)return;
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
            for(int d=0;d<2;d++)set(a+dx,floor-1-d,c+dz,3);
            set(a+dx,floor,c+dz,16);
            for(int dy=1;dy<=3;dy++)set(a+dx,floor+dy,c+dz,(Math.abs(dx)==2||Math.abs(dz)==2)?(Math.abs(dx)==2&&Math.abs(dz)==2?4:16):0);
            set(a+dx,floor+4,c+dz,4);
        }
        set(a,floor+1,c-2,0);set(a,floor+2,c-2,0);set(a+1,floor+2,c+2,17);set(a-1,floor+1,c,21);
    }
    void makeRuin(int a,int c){int floor=groundTop(a,c);if(floor+5>=H)return;
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
            set(a+dx,floor,c+dz,7);
            if(Math.abs(dx)==3||Math.abs(dz)==3){int hh=1+(Math.abs(dx*7+dz*11)%4);for(int h=1;h<=hh;h++)set(a+dx,floor+h,c+dz,h%3==0?25:7);}
        }
        set(a,floor+1,c,8);
    }
    public boolean collides(float px,float py,float pz){
        if(px<.3f||pz<.3f||px>N-.3f||pz>N-.3f||py<1)return true;
        for(int a=(int)Math.floor(px-.28f);a<=(int)Math.floor(px+.28f);a++)
        for(int b=(int)Math.floor(py);b<=(int)Math.floor(py+1.7f);b++)
        for(int c=(int)Math.floor(pz-.28f);c<=(int)Math.floor(pz+.28f);c++)if(!passable(get(a,b,c)))return true;
        return false;
    }
    public void step(float dt,float side,float forward,boolean jump){step(dt,side,forward,jump,false,false);}
    public void step(float dt,float side,float forward,boolean jump,boolean sprint,boolean crouch){
        crouching=crouch;sprinting=sprint&&!crouch&&forward>.15f&&hunger>1;
        float n=(float)Math.sqrt(side*side+forward*forward);if(n>1){side/=n;forward/=n;}
        float rad=(float)Math.toRadians(yaw),v=(crouch?1.65f:sprinting?6.4f:4.3f)*dt;
        float dx=((float)Math.cos(rad)*side+(float)Math.sin(rad)*forward)*v;
        float dz=((float)Math.sin(rad)*side-(float)Math.cos(rad)*forward)*v;
        if(!collides(x+dx,y,z) && (!crouch || !grounded || hasSupport(x+dx,z)))x+=dx;
        if(!collides(x,y,z+dz) && (!crouch || !grounded || hasSupport(x,z+dz)))z+=dz;
        if(jump&&grounded){vy=7;grounded=false;}vy-=18*dt;
        float dy=vy*dt;int steps=Math.max(1,(int)(Math.abs(dy)/.08f)+1);grounded=false;
        for(int i=0;i<steps;i++){if(!collides(x,y+dy/steps,z))y+=dy/steps;else{if(vy<0)grounded=true;vy=0;break;}}
        if(y<-5){y=groundTop((int)x,(int)z)+.1f;vy=0;}
        dayTime=(dayTime+dt/300f)%1;
        lifeClock+=dt;mobClock+=dt;
        if(lifeClock>12){lifeClock=0;if(!creative){hunger=Math.max(0,hunger-.5f);if(hunger==0)health=Math.max(1,health-1);}}
        for(Mob m:mobs){
            m.phase+=dt*(m.type==2?1.5f:.55f);
            if(mobClock>1.5f){float move=.36f;
                float nx=m.x+(float)Math.sin(m.phase)*move,nz=m.z+(float)Math.cos(m.phase)*move;
                if(nx>2&&nz>2&&nx<N-2&&nz<N-2){int gy=groundTop((int)nx,(int)nz);
                    if(Math.abs(gy-m.y)<1.1f&&get((int)nx,gy,(int)nz)==0){m.x=nx;m.z=nz;m.y=gy;}}
                if(!creative&&m.type==2&&Math.hypot(x-m.x,z-m.z)<1.5f&&Math.abs(y-m.y)<2)health=Math.max(1,health-1);
            }
        }
        if(mobClock>1.5f)mobClock=0;
    }
    private boolean hasSupport(float px,float pz){
        // Sneaking keeps feet supported near cliff edges.
        return !passable(get((int)Math.floor(px),(int)Math.floor(y-.12f),(int)Math.floor(pz)));
    }
    public int[] ray(){
        double ya=Math.toRadians(yaw),pi=Math.toRadians(pitch);
        float dx=(float)(Math.sin(ya)*Math.cos(pi)),dy=(float)Math.sin(pi),dz=(float)(-Math.cos(ya)*Math.cos(pi));
        int ax=(int)Math.floor(x),ay=(int)Math.floor(y+cameraHeight()),az=(int)Math.floor(z);
        for(float t=0;t<6;t+=.035f){int bx=(int)Math.floor(x+dx*t),by=(int)Math.floor(y+cameraHeight()+dy*t),bz=(int)Math.floor(z+dz*t);
            if(get(bx,by,bz)!=0&&get(bx,by,bz)!=9)return new int[]{bx,by,bz,ax,ay,az};ax=bx;ay=by;az=bz;
        }return null;
    }
    public boolean edit(boolean place){int[] hit=ray();if(hit==null)return false;
        if(!place){int t=get(hit[0],hit[1],hit[2]);if(t==23)return false;
            set(hit[0],hit[1],hit[2],0);lastX=hit[0];lastZ=hit[2];
            if(!creative){int drop=t;if(t==1)drop=2;if(t==5&&gameRandom.nextInt(5)==0)bag[28]++;if(drop>0&&drop<=MAX_BLOCK)bag[drop]++;}
            return true;
        }
        int a=hit[3],b=hit[4],c=hit[5];
        if(selected<1||selected>MAX_BLOCK||a<0||a>=N||b<1||b>=H||c<0||c>=N||get(a,b,c)!=0)return false;
        if(!creative&&bag[selected]==0)return false;
        set(a,b,c,selected);if(collides(x,y,z)){set(a,b,c,0);return false;}
        if(!creative)bag[selected]--;lastX=a;lastZ=c;return true;
    }
    public boolean craft(int recipeIndex){if(recipeIndex<0||recipeIndex>=RECIPES.length)return false;
        int[] r=RECIPES[recipeIndex];if(!creative){for(int j=2;j<r.length;j+=2)if(bag[r[j]]<r[j+1])return false;
            for(int j=2;j<r.length;j+=2)bag[r[j]]-=r[j+1];}
        bag[r[0]]+=r[1];return true;
    }
    public boolean eat(){if(creative)return false;int id=bag[30]>0?30:bag[28]>0?28:bag[29]>0?29:0;if(id==0)return false;
        bag[id]--;hunger=Math.min(20,hunger+(id==30?8:id==29?3:4));return true;}
    public boolean hitMob(){double ya=Math.toRadians(yaw);float fx=(float)Math.sin(ya),fz=(float)-Math.cos(ya);
        for(int i=0;i<mobs.size();i++){Mob m=mobs.get(i);float dx=m.x-x,dz=m.z-z;float dist=(float)Math.hypot(dx,dz);
            if(dist<3.2f&&Math.abs(m.y-y)<2&&dist>0&& (dx*fx+dz*fz)/dist>.55f){
                if(!creative&&m.type!=2)bag[29]+=2;mobs.remove(i);return true;
            }
        }return false;
    }
    public void choose(int id){if(id>0&&id<=MAX_BLOCK){selected=id;hotbar[hotbarIndex]=id;}}
    public void save(File file)throws IOException{
        File tmp=new File(file.getPath()+".tmp");
        try(DataOutputStream d=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)))){
            d.writeInt(0x56475232);d.writeInt(seed);d.writeFloat(x);d.writeFloat(y);d.writeFloat(z);d.writeFloat(yaw);d.writeFloat(pitch);d.writeInt(selected);d.write(blocks);
            d.writeBoolean(creative);d.writeFloat(health);d.writeFloat(hunger);d.writeFloat(dayTime);d.writeInt(hotbarIndex);
            for(int t:hotbar)d.writeInt(t);for(int t:bag)d.writeInt(t);
            d.writeInt(mobs.size());for(Mob m:mobs){d.writeInt(m.type);d.writeFloat(m.x);d.writeFloat(m.y);d.writeFloat(m.z);d.writeFloat(m.phase);}
        }
        if(file.exists()&&!file.delete())throw new IOException("Cannot replace save");
        if(!tmp.renameTo(file))throw new IOException("Cannot commit save");
    }
    public boolean load(File file){try(DataInputStream d=new DataInputStream(new BufferedInputStream(new FileInputStream(file)))){
        int magic=d.readInt();if(magic!=0x56475231&&magic!=0x56475232)return false;
        int s=d.readInt();float px=d.readFloat(),py=d.readFloat(),pz=d.readFloat(),ya=d.readFloat(),pi=d.readFloat();int sel=d.readInt();byte[] data=new byte[N*H*N];d.readFully(data);
        if(!Float.isFinite(px)||!Float.isFinite(py)||!Float.isFinite(pz)||!Float.isFinite(ya)||!Float.isFinite(pi)||px<.3||px>N-.3||pz<.3||pz>N-.3||py<1||py>H+10||sel<1||sel>MAX_BLOCK)return false;
        for(byte t:data)if((t&255)>MAX_BLOCK)return false;
        boolean cr=true;float hp=20,food=20,time=.25f;int hi=0;int[] hb={1,2,3,4,6,16,17,21};int[] items=new int[MAX_ITEM+1];ArrayList<Mob> ms=new ArrayList<>();
        if(magic==0x56475231)hb[0]=sel;
        if(magic==0x56475232){cr=d.readBoolean();hp=d.readFloat();food=d.readFloat();time=d.readFloat();hi=d.readInt();
            if(!Float.isFinite(hp)||hp<0||hp>20||!Float.isFinite(food)||food<0||food>20||!Float.isFinite(time)||time<0||time>1||hi<0||hi>=8)return false;
            for(int i=0;i<hb.length;i++){hb[i]=d.readInt();if(hb[i]<1||hb[i]>MAX_BLOCK)return false;}
            for(int i=0;i<items.length;i++){items[i]=d.readInt();if(items[i]<0||items[i]>1000000)return false;}
            int qty=d.readInt();if(qty<0||qty>100)return false;
            for(int i=0;i<qty;i++){int t=d.readInt();float xx=d.readFloat(),yy=d.readFloat(),zz=d.readFloat(),p=d.readFloat();
                if(t<0||t>2||!Float.isFinite(xx)||!Float.isFinite(yy)||!Float.isFinite(zz)||!Float.isFinite(p)||xx<0||xx>N||zz<0||zz>N||yy<0||yy>H+1)return false;
                ms.add(new Mob(t,xx,yy,zz,p));}
        }
        seed=s;x=px;y=py;z=pz;yaw=ya;pitch=Math.max(-85,Math.min(85,pi));selected=sel;blocks=data;vy=0;creative=cr;health=hp;hunger=food;dayTime=time;hotbarIndex=hi;
        System.arraycopy(hb,0,hotbar,0,8);System.arraycopy(items,0,bag,0,items.length);mobs.clear();mobs.addAll(ms);return true;
        }catch(IOException e){return false;}}
}
