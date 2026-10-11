package ua.waserko.voxelgrove;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import java.io.InputStream;
import java.nio.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Fixed-pipeline ES 1.0: 3x3 updateable meshes, 16px procedural atlas, animated creatures. */
public final class GameRenderer implements GLSurfaceView.Renderer {
    final World world;final MainActivity owner;
    volatile float side,forward;volatile boolean jump,paused=true;
    static final int CH=16,COLS=World.N/CH;
    private static final class Mesh {FloatBuffer pos,shade,uv;int count;}
    private final Mesh[] chunks=new Mesh[COLS*COLS];
    private final boolean[] dirty=new boolean[COLS*COLS];
    private FloatBuffer cube;private int texId;
    private long last,lastSave;
    static final int[][] D={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    static final float[][] F={
        {1,0,0,1,1,0,1,1,1,1,0,1}, {0,0,1,0,1,1,0,1,0,0,0,0},
        {0,1,0,0,1,1,1,1,1,1,1,0}, {0,0,1,0,0,0,1,0,0,1,0,1},
        {1,0,1,1,1,1,0,1,1,0,0,1}, {0,0,0,0,1,0,1,1,0,1,0,0}};
    private static final int[] TRI={0,1,2,0,2,3};
    GameRenderer(World w,MainActivity a){world=w;owner=a;invalidateAll();}
    void invalidateAll(){for(int i=0;i<dirty.length;i++)dirty[i]=true;}
    void invalidateBlock(int x,int z){if(x<0||z<0){invalidateAll();return;}
        int cx=x/CH,cz=z/CH;for(int a=Math.max(0,cx-1);a<=Math.min(COLS-1,cx+1);a++)for(int b=Math.max(0,cz-1);b<=Math.min(COLS-1,cz+1);b++)dirty[a*COLS+b]=true;
    }
    public void onSurfaceCreated(GL10 g,EGLConfig config){
        g.glClearColor(.55f,.76f,.91f,1);g.glEnable(GL10.GL_DEPTH_TEST);g.glDisable(GL10.GL_CULL_FACE);
        g.glEnableClientState(GL10.GL_VERTEX_ARRAY);g.glEnableClientState(GL10.GL_COLOR_ARRAY);
        g.glEnable(GL10.GL_FOG);g.glFogf(GL10.GL_FOG_MODE,GL10.GL_LINEAR);g.glFogf(GL10.GL_FOG_START,18);g.glFogf(GL10.GL_FOG_END,48);
        loadTexture(g);cube=makeCube();invalidateAll();last=System.nanoTime();
    }
    private void loadTexture(GL10 g){
        int[] ids=new int[1];g.glGenTextures(1,ids,0);texId=ids[0];g.glBindTexture(GL10.GL_TEXTURE_2D,texId);
        g.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_NEAREST);
        g.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_MAG_FILTER,GL10.GL_NEAREST);
        g.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_WRAP_S,GL10.GL_CLAMP_TO_EDGE);
        g.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_WRAP_T,GL10.GL_CLAMP_TO_EDGE);
        try(InputStream in=owner.getAssets().open("terrain.png")){
            Bitmap bmp=BitmapFactory.decodeStream(in);if(bmp!=null){GLUtils.texImage2D(GL10.GL_TEXTURE_2D,0,bmp,0);bmp.recycle();}
        }catch(Exception ignored){}
    }
    public void onSurfaceChanged(GL10 g,int w,int h){g.glViewport(0,0,w,h);g.glMatrixMode(GL10.GL_PROJECTION);g.glLoadIdentity();
        GLU.gluPerspective(g,72,(float)w/Math.max(1,h),.08f,72);g.glMatrixMode(GL10.GL_MODELVIEW);
    }
    public void onDrawFrame(GL10 g){long now=System.nanoTime();float dt=Math.min(.035f,(now-last)/1e9f);last=now;
        if(!paused){world.step(dt,side,forward,jump);jump=false;if(now-lastSave>15_000_000_000L){owner.saveWorld();lastSave=now;}}
        float sun=(float)Math.max(.20,.5+.5*Math.sin(2*Math.PI*world.dayTime));
        float skyR=.035f+sun*.54f,skyG=.065f+sun*.72f,skyB=.13f+sun*.79f;
        g.glClearColor(skyR,skyG,skyB,1);g.glFogfv(GL10.GL_FOG_COLOR,new float[]{skyR,skyG,skyB,1},0);
        g.glClear(GL10.GL_COLOR_BUFFER_BIT|GL10.GL_DEPTH_BUFFER_BIT);g.glLoadIdentity();
        double ya=Math.toRadians(world.yaw),pi=Math.toRadians(world.pitch);
        GLU.gluLookAt(g,world.x,world.y+1.55f,world.z,world.x+(float)(Math.sin(ya)*Math.cos(pi)),
            world.y+1.55f+(float)Math.sin(pi),world.z-(float)(Math.cos(ya)*Math.cos(pi)),0,1,0);
        for(int a=0;a<chunks.length;a++)if(dirty[a]){buildChunk(a/COLS,a%COLS);dirty[a]=false;}
        g.glEnable(GL10.GL_TEXTURE_2D);g.glBindTexture(GL10.GL_TEXTURE_2D,texId);g.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        for(Mesh m:chunks)if(m!=null&&m.count>0){g.glVertexPointer(3,GL10.GL_FLOAT,0,m.pos);g.glColorPointer(4,GL10.GL_FLOAT,0,m.shade);
            g.glTexCoordPointer(2,GL10.GL_FLOAT,0,m.uv);g.glDrawArrays(GL10.GL_TRIANGLES,0,m.count);}
        g.glDisable(GL10.GL_TEXTURE_2D);g.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        drawMobs(g);
        int[] hit=world.ray();if(hit!=null)outline(g,hit);
    }
    static FloatBuffer buffer(float[] f,int n){FloatBuffer b=ByteBuffer.allocateDirect(n*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(f,0,n).position(0);return b;}
    private void buildChunk(int cx,int cz){int faces=0,x0=cx*CH,z0=cz*CH,x1=Math.min(World.N,x0+CH),z1=Math.min(World.N,z0+CH);
        for(int x=x0;x<x1;x++)for(int y=0;y<World.H;y++)for(int z=z0;z<z1;z++)if(world.get(x,y,z)>0)
            for(int[] d:D)if(world.get(x+d[0],y+d[1],z+d[2])==0||
                (world.get(x,y,z)!=9&&world.get(x+d[0],y+d[1],z+d[2])==9))faces++;
        float[] vv=new float[faces*18],cc=new float[faces*24],uv=new float[faces*12];int vi=0,ci=0,ti=0;
        float[] shades={.80f,.68f,1,.49f,.87f,.74f};
        for(int x=x0;x<x1;x++)for(int y=0;y<World.H;y++)for(int z=z0;z<z1;z++){
            int t=world.get(x,y,z);if(t==0)continue;
            for(int face=0;face<6;face++){
                int[] d=D[face];int other=world.get(x+d[0],y+d[1],z+d[2]);if(other!=0&&!(t!=9&&other==9))continue;
                float sh=shades[face]*(.95f+((x*71+y*37+z*13)&7)*.008f);
                int tile=t==1&&face!=2?28:t==4&&(face==2||face==3)?29:t==19&&face!=2?30:t;
                float u0=(tile%8)/8f+.0005f,u1=(tile%8+1)/8f-.0005f;
                float v0=(tile/8)/4f+.001f,v1=(tile/8+1)/4f-.001f;
                float[] quadUV={u1,v1,u1,v0,u0,v0,u0,v1};
                for(int k:TRI){vv[vi++]=x+F[face][k*3];vv[vi++]=y+F[face][k*3+1];vv[vi++]=z+F[face][k*3+2];
                    cc[ci++]=sh;cc[ci++]=sh;cc[ci++]=sh;cc[ci++]=1;
                    uv[ti++]=quadUV[2*k];uv[ti++]=quadUV[2*k+1];}
            }
        }
        Mesh m=new Mesh();m.pos=buffer(vv,vi);m.shade=buffer(cc,ci);m.uv=buffer(uv,ti);m.count=vi/3;chunks[cx*COLS+cz]=m;
    }
    private FloatBuffer makeCube(){float[] a=new float[108];int n=0;for(int f=0;f<6;f++)for(int k:TRI){a[n++]=F[f][k*3]-.5f;a[n++]=F[f][k*3+1];a[n++]=F[f][k*3+2]-.5f;}return buffer(a,a.length);}
    private void drawMobs(GL10 g){g.glDisableClientState(GL10.GL_COLOR_ARRAY);g.glVertexPointer(3,GL10.GL_FLOAT,0,cube);
        for(World.Mob m:world.mobs){float[] rgb=m.type==0?new float[]{.94f,.91f,.80f}:m.type==1?new float[]{.72f,.47f,.36f}:new float[]{.36f,.27f,.52f};
            g.glColor4f(rgb[0],rgb[1],rgb[2],1);box(g,m.x,m.y+.28f,m.z,.8f,.65f,.55f);
            g.glColor4f(rgb[0]*.9f,rgb[1]*.9f,rgb[2]*.9f,1);box(g,m.x,m.y+.64f,m.z-.42f,.50f,.55f,.48f);
            for(int dx=-1;dx<=1;dx+=2)for(int dz=-1;dz<=1;dz+=2)box(g,m.x+dx*.22f,m.y,m.z+dz*.17f,.18f,.32f,.18f);
        }
        g.glEnableClientState(GL10.GL_COLOR_ARRAY);
    }
    private void box(GL10 g,float x,float y,float z,float w,float h,float d){g.glPushMatrix();g.glTranslatef(x,y,z);g.glScalef(w,h,d);g.glDrawArrays(GL10.GL_TRIANGLES,0,36);g.glPopMatrix();}
    void outline(GL10 g,int[] h){float[] p={0,0,0,1,0,0,1,1,0,0,1,0,0,0,1,1,0,1,1,1,1,0,1,1};int[] lines={0,1,1,2,2,3,3,0,4,5,5,6,6,7,7,4,0,4,1,5,2,6,3,7};float[] v=new float[72];int j=0;
        for(int i:lines)for(int a=0;a<3;a++)v[j++]=h[a]+(p[i*3+a]-.5f)*1.006f+.5f;
        g.glDisableClientState(GL10.GL_COLOR_ARRAY);g.glColor4f(1,1,.80f,1);g.glLineWidth(2);g.glVertexPointer(3,GL10.GL_FLOAT,0,buffer(v,v.length));g.glDrawArrays(GL10.GL_LINES,0,24);g.glEnableClientState(GL10.GL_COLOR_ARRAY);
    }
}
