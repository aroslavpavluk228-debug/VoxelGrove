package ua.waserko.voxelgrove;
import java.io.*;
import java.util.*;

/** Runs with stock JDK: no Android SDK needed. */
public class WorldTest {
 static int count;
 static boolean hasBlock(World w,int block){for(byte b:w.blocks)if((b&255)==block)return true;return false;}
 static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);count++;System.out.println("PASS "+s);}
 public static void main(String[] a)throws Exception{
  World w=new World();w.generate(2026);World v=new World();v.generate(2026);
  check(Arrays.equals(w.blocks,v.blocks),"deterministic world");
  check(w.mobs.size()==v.mobs.size(),"deterministic creature count");
  check(!w.collides(w.x,w.y,w.z),"spawn clear");
  check(hasBlock(w,16),"cabin generated");
  check(hasBlock(w,7),"ruin generated");
  check(w.get(0,0,0)==23,"bedrock base");
  int ore=0;for(byte t:w.blocks)if(t==10||t==11||t==12||t==13||t==14||t==27)ore++;
  check(ore>50,"ore generation");
  for(int i=0;i<100;i++)w.step(.016f,0,0,false);check(w.grounded,"gravity lands");
  float y=w.y;w.step(.016f,0,0,true);check(w.y>y,"jump rises");
  w.blocks=new byte[World.N*World.H*World.N];w.x=10.5f;w.y=2;w.z=10.5f;w.yaw=0;w.pitch=0;
  w.set(10,3,7,3);check(w.ray()!=null&&w.ray()[2]==7,"ray selects block");
  w.selected=7;check(w.edit(true)&&w.get(10,3,8)==7,"places adjacent block");
  check(w.edit(false)&&w.get(10,3,8)==0,"breaks nearest block");
  w.set(10,3,9,3);check(!w.edit(true)&&w.get(10,3,10)==0,"cannot build inside player");
  check(w.collides(-1,2,10),"world boundary solid");
  check(!w.collides(10.5f,2,12.5f),"empty cell passable");
  w.set(10,2,12,9);check(!w.collides(10.5f,2,12.5f),"water passable");
  w.creative=false;Arrays.fill(w.bag,0);
  check(!w.craft(0),"can't craft without materials");
  w.bag[4]=2;check(w.craft(0)&&w.bag[4]==1&&w.bag[16]==4,"wood converts into 4 planks");
  w.bag[10]=1;w.bag[29]=1;check(w.craft(6)&&w.bag[30]==1&&w.bag[10]==0,"food cooking consumes inputs");
  w.hunger=8;check(w.eat()&&w.hunger==16,"eating restores hunger");
  w.hotbarIndex=2;w.choose(20);check(w.hotbar[2]==20&&w.selected==20,"hotbar customized");
  File f=File.createTempFile("grove-test",".vg");w.save(f);
  check(v.load(f)&&Arrays.equals(w.blocks,v.blocks)&&v.selected==20,"save/load roundtrip");
  check(!v.creative&&v.hotbar[2]==20&&v.bag[16]==4&&v.hunger==16,"survival save state");
  try(FileOutputStream out=new FileOutputStream(f)){out.write(1);}check(!v.load(f),"corrupt save rejected");
  File old=File.createTempFile("grove-v1",".vg");
  try(DataOutputStream d=new DataOutputStream(new FileOutputStream(old))){d.writeInt(0x56475231);d.writeInt(17);d.writeFloat(12.5f);d.writeFloat(5);d.writeFloat(12.5f);d.writeFloat(0);d.writeFloat(0);d.writeInt(7);d.write(new byte[World.N*World.H*World.N]);}
  check(v.load(old)&&v.selected==7&&v.hotbar[0]==7&&v.creative,"old V1 saves migrate");
  World motion=new World();motion.blocks=new byte[World.N*World.H*World.N];
  motion.x=20.5f;motion.y=3;motion.z=20.5f;motion.yaw=0;motion.hunger=20;
  motion.step(.05f,0,1,false,false,false);float walking=20.5f-motion.z;
  motion.z=20.5f;motion.step(.05f,0,1,false,true,false);float running=20.5f-motion.z;
  check(running>walking*1.4f,"sprint is faster than walking");
  motion.z=20.5f;motion.step(.05f,0,1,false,false,true);float sneaking=20.5f-motion.z;
  check(sneaking<walking*.55f,"sneak is slower than walking");
  check(motion.crouching&&Math.abs(motion.cameraHeight()-1.12f)<.01f,"sneak lowers viewpoint");
  motion.step(.05f,0,0,false,false,false);
  check(Math.abs(motion.cameraHeight()-1.55f)<.01f,"standing restores viewpoint");
  f.delete();old.delete();System.out.println(count+" checks passed");
 }
}
