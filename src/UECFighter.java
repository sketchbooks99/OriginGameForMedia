import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.applet.AudioClip;
import java.io.InputStream;
import java.io.IOException;

import java.util.*;
import java.util.List;

//操作キャラベース-----------------------------------------------------------

abstract class UECPlayerBase  {
    private final int K_UP, K_DOWN, K_LEFT, K_RIGHT, K_WeakAttack, K_StrongAttack;//操作ボタンコード
    protected boolean bK_UP = false, bK_DOWN = false, bK_LEFT = false, bK_RIGHT = false, bK_WeakAttack = false, bK_StrongAttack = false;//操作ボタンのフラグ
    protected boolean is_Jump = false, is_HighJump = false, is_Dash = false, is_Squat = false, is_Down, did_PlayWin = false;//ジャンプ,二段ジャンプ中かダッシュ中かしゃがみ中かダウン中かどうか
    protected int walkCount;//歩いている時に足踏みを管理する

    protected boolean canAttack = true, canDown = false;//攻撃行動できるか,ダウンできるかどうか
    protected int canCombo = 0, attackId = -1;//コンボ技の入力受付時間を管理, 前回攻撃idを引き継ぐ(-1はダミーコード)
    protected AttackInfo attackInfo;//攻撃情報を収納する

    protected boolean before_Right = false;//直前の左右入力を保存する
    protected boolean canBlock = false;//ガード受付状態の管理
    protected int HP;//キャラの体力
    protected Point2D.Float position; //キャラの座標
    protected float dy, dx;
    protected Point size, range, startRange;//キャラのサイズ, あたり範囲のサイズ, あたり判定のスタート(右向き基準, 左向き基準)
    protected float magnification;//キャラサイズの倍率

    protected int underLine, rightLine;//ステージ下限右限
    protected int stun, canDash;//行動不自由時間, ダッシュ状態を受け付けている時間
    protected Looking look; //右を向いているか
    protected UECPlayerBase OpponentPlayer;//対戦相手

    protected TreeMap<String, Image> Images;//グラフィック管理
    protected String NowImageName;//呼び出す画像の名前
    protected List<String> NowRequestedPlayAudios;//リクエストする名前

    protected boolean Debugmessage = false;

    public UECPlayerBase(int K_UP, int K_DOWN, int K_LEFT, int K_RIGHT, int K_WeakAttack, int K_StrongAttack, float positionX, float magnification, Point size, Point range, Point startRange, boolean is_Right){
        //操作ボタンコード
        this.K_UP = K_UP;
        this.K_DOWN = K_DOWN;
        this.K_LEFT = K_LEFT;
        this.K_RIGHT = K_RIGHT;
        this.K_WeakAttack = K_WeakAttack;
        this.K_StrongAttack = K_StrongAttack;

        if(is_Right){
            look = Looking.Right;
        }else{
            look = Looking.Left;
        }

        this.magnification = magnification;

        underLine = UECFighter.SCREEN_HEIGHT * 9 / 10 - size.y;
        rightLine = UECFighter.SCREEN_WIDTH - size.x;

        //座標設定
        position = new Point2D.Float(positionX, underLine);
        this.size = size;
        this.range = range;
        this.startRange = startRange;

        //画像設定
        Images = new TreeMap<String, Image>();
        RegisterImage();

        //音声設定
        NowRequestedPlayAudios = new ArrayList<String>();
    }

    public void keyPressed(KeyEvent e) {
        if(stun > 0){return;}
        int keyCode = e.getKeyCode();
        if(keyCode == K_UP && !bK_UP){
            if(bK_UP = !bK_DOWN && !attackInfo.isHaving()){
                Jump(true);
                canBlock = false;
            }
            dx = 0;
        } else
        if(keyCode == K_DOWN && !bK_DOWN){ if(bK_DOWN = !bK_UP){ Squat(true); } } else
        if(keyCode == K_RIGHT && !bK_RIGHT){
            if(bK_RIGHT = !bK_LEFT && !attackInfo.isHaving()){
                Move(true, true);
                canBlock = look == Looking.Left;
            }
        } else
        if(keyCode == K_LEFT && !bK_LEFT){
            if(bK_LEFT = !bK_RIGHT && !attackInfo.isHaving()){
                Move(false, true);
                canBlock = look == Looking.Right;
            }
        } else
        if(keyCode == K_WeakAttack && !bK_WeakAttack){
            bK_WeakAttack = true;
            WeakAttack(true);
        } else if(keyCode == K_StrongAttack && !bK_StrongAttack){
            bK_StrongAttack = true;
            StrongAttack(true);
        }
    }

    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if(keyCode == K_UP){ bK_UP = false; } else
        if(keyCode == K_DOWN){ bK_DOWN = false; dx /= 2f;} else
        if(keyCode == K_RIGHT){ bK_RIGHT = false; if(look == Looking.Left){canBlock = false;}} else
        if(keyCode == K_LEFT){ bK_LEFT = false; if(look == Looking.Right){canBlock = false;}} else
        if(keyCode == K_WeakAttack){ bK_WeakAttack = false; } else
        if(keyCode == K_StrongAttack){ bK_StrongAttack = false; }
    }

    //ジャンプモーション
    protected void Jump(boolean Pressed){
        if(Pressed && (!is_Jump || !is_HighJump)){
            if(!is_Jump){
                is_Jump = true;
                NowImageName = "Jump";
            }else{
                is_HighJump = true;
            }
            dy = -30.0f;
        }
    }
    //しゃがみモーション
    protected void Squat(boolean Pressed){
        if (Pressed){
            if(is_Jump){dx *= 0.5f;}
            is_Dash = false;
        }
        else if((is_Squat = !is_Jump) && dx != 0.0f){dx *= 0.3f;}
    }
    //弱攻撃
    abstract protected void WeakAttack(boolean Pressed);
    //強攻撃
    abstract protected void StrongAttack(boolean Pressed);
    //移動
    protected void Move(boolean is_Right, boolean Pressed){

    }
    //一連の行動を管理する
    public void Action(){

        if(canAttack && stun < 0){
            if(!attackInfo.isHaving()) {

                if (attackId != -1 && canCombo-- <= 0) {
                    attackId = -1;
                }
                //弱アタック・強アタック
                if (bK_WeakAttack) {
                    WeakAttack(false);
                } else if (bK_StrongAttack) {
                    StrongAttack(false);
                }
                //左右入力
                if (bK_LEFT || bK_RIGHT) {
                    Move(bK_RIGHT, false);
                } else {
                    canDash--;
                    if (is_Dash) {
                        is_Dash = false;
                    }
                    if (!is_Jump) {
                        dx = 0;
                        if(!is_Down) {
                            NowImageName = "Stand";
                    }
                }
                }

                if (bK_UP) {
                    Jump(false);
                } else if (bK_DOWN) {
                    Squat(false);
                }
            }

        }else{

            canDown = stun-- != 0 && canDown;
        }

        //横方向移動
        position.x += dx;
        if(position.x<0){
            dx = 0;
            position.x = 0;
        }else if(position.x>rightLine){
            dx = 0;
            position.x = rightLine;
        }

        //落下処理
        if(is_Jump) {
            canBlock = false;
            position.y += dy;
            dy += 2.0f;
            if (position.y >= underLine) {
                if(canDown && NowImageName.equals("Damage")){
                    NowImageName = "Down";
                }
                is_Jump = false;
                is_HighJump = false;
                position.y = underLine;
                dx = 0;
                //ここで反転処理を行う
                canBlock = (look == Looking.Left && bK_RIGHT) || (look == Looking.Right && bK_LEFT);
            }
        }
    }

    abstract public void RegisterImage();
    abstract public void RegisterAudioClip(TreeMap<String, AudioClip> audios);

    public Image getNowImage(){ return Images.get(NowImageName); }
    public void setNowImageName(String nowImageName){ NowImageName = nowImageName; }
    public List<String> getNowRequestedPlayAudio(){
        return NowRequestedPlayAudios;
    }

    public Point2D.Float getPosition(){ return position; }

    //ダメージ情報を扱う
    public boolean ConfirmDamage(Point2D.Float Force, int Stun, int damage, boolean canDown){
        if(canBlock){
            //ブロック出来る時
            NowRequestedPlayAudios.add("guard");
            NowImageName = "Guard";

            HP -= damage/5;
            stun = 8;
            dx = 0;
        }else{
            //出来ない時
            NowRequestedPlayAudios.add("Punch");
            NowImageName = "Damage";
            this.canDown = canDown;
            HP -= damage;
            stun = Stun;
            canCombo = 0;
            attackId = -1;
            dx = Force.x;
            dy = Force.y;
            is_Jump |= dy < 0f;//is_Jumpがfalseの時上方向加力するならtrueに
            canAttack = true;
        }
        canCombo = 0; attackId = -1;
        return HP>0;
    }

    public Point getMySize(){return size;}
    public Point getRange(){ return  range;}
    public Point getStartRange(){return startRange;}

    //向き情報を設定し返す
    public Looking setgetLook(Point2D.Float OpponentPos){
       if (!is_Jump){
           if(position.x <= OpponentPos.x){
               canBlock = bK_LEFT;
               return look = Looking.Right;
           }else{
               canBlock = bK_RIGHT;
               return look = Looking.Left;
           }
       }
       return look;
    }
    public Looking getLook(){ return look; }

    public int getHP(){ return HP; }
    public void setHP(int HP){ this.HP = HP; }

    public void setOpponentPlayer(UECPlayerBase opponentPlayer) { OpponentPlayer = opponentPlayer; }

    public AttackInfo getAttackInfo(){ return attackInfo; }
    public void setAttackInfo(UECPlayerBase Opponent){ attackInfo = new AttackInfo(this, Opponent, magnification); }
    public void setCanAttack(boolean canAttack){ this.canAttack = canAttack; }

    public boolean getCanDown(){ return  canDown; }

    abstract public void GameFinished(int WinOrLoseOrDraw, boolean HPeqZero);

    //デバッグ
    public void setDebugmessage(boolean debugmessage){Debugmessage = debugmessage;}

}

//操作キャラA
class NaoChan extends UECPlayerBase{

    public NaoChan(int K_UP, int K_DOWN, int K_LEFT, int K_RIGHT, int K_WeakAttack, int K_StrongAttack, float positionX, float positionY, float magnification, Point size, Point range, Point startRange, boolean is_Right){
        super(K_UP, K_DOWN, K_LEFT, K_RIGHT, K_WeakAttack, K_StrongAttack, positionX, magnification, size, range, startRange, is_Right);
        HP = 100;//体力セット
    }

    @Override
    protected void Jump(boolean Pressed) {
        super.Jump(Pressed);
            }

    @Override
    protected void Squat(boolean Pressed) {
        super.Squat(Pressed);
        }

    @Override
    protected void WeakAttack(boolean Pressed) {
        /*
        attackInfo.setInfo(
        攻撃範囲の始点[右向き基準],
        攻撃範囲,
        int Occurrence… 攻撃発生までの時間,
        int Duration… 攻撃の継続時間,
        int Interval… 次の攻撃入力を受け付けない時間,
        int ContinuousHit… 多段ヒット攻撃ならその間隔をセットする[継続時間中は指定時間経過で攻撃判定復活],
        int Damage… ダメージ,
        Point2D.Float Force… 攻撃時に相手を吹っ飛ばす向き[右向き基準],
        int stun… 攻撃を受けてひるむ時間,
        int id… 攻撃のid)
         */
        if(canAttack) {
            if (Pressed) {
                if(!is_Jump) {
                    //地上技
                    dx = 0f;
                    switch (attackId) {
                        default:
                            NowImageName = "Punch1";
                            NowRequestedPlayAudios.add("Nao_punch1");
                            attackInfo.setInfo(new Point(80, 30), new Point(30, 20), 0, 5, 0, 9999, 10, new Point2D.Float(3f, -5f), 5, 1, false);
                            attackId = 1;
                            canCombo = 7;
                            canAttack = false;
                            break;
                        case 1:
                            NowImageName = "Punch2";
                            NowRequestedPlayAudios.add("Nao_punch2");
                            attackInfo.setInfo(new Point(80, 0), new Point(20, 30), 0, 5, 0, 9999, 10, new Point2D.Float(3f, -10f), 5, 2, false);
                            attackId = 2;
                            canCombo = 7;
                            canAttack = false;
                            break;
                        case 2:
                            NowImageName = "Kick3";
                            NowRequestedPlayAudios.add("Nao_kick3");
                            attackInfo.setInfo(new Point(70, 30), new Point(50, 30), 0, 20, 10, 9999, 10, new Point2D.Float(30f, -20f), 40, -1 ,true);
                            attackId = -1;
                            canCombo = 0;
                            canAttack = false;
                            break;
                    }
                }else{
                    //空中技
                    switch (attackId) {
                        default:
                            NowImageName = "FlyingKick";
                            NowRequestedPlayAudios.add("Nao_kick3");
                            attackInfo.setInfo(new Point(80, 0), new Point(20, 30), 0, 10, 10, 9999, 5, new Point2D.Float(20f, 0f), 2, 2, false);
                            attackId = -1;
                            canCombo = 0;
                            break;


                    }
                }

            }
        }
    }

    @Override
    protected void StrongAttack(boolean Pressed) {
        if(canAttack) {
            if (Pressed) {
                switch (attackId) {
                    default:
                        NowImageName = "Guard";
                        attackInfo.setInfo(new Point(0, 0), new Point(0, 0), 10, 0, 0, 9999,0, new Point2D.Float(0f,0f), 0, 11, false);
                        attackId = 11;
                        canCombo = 20;
                        canAttack = false;
                        break;
                    case 11:
                        NowImageName = "StrongPunch";
                        attackInfo.setInfo(new Point(70, 50), new Point(50, 20), 0, 15, 15, 5, 8, new Point2D.Float(25f, -10f), 20, -1 ,false);
                        attackId = -1;
                        canCombo = 0;
                        canAttack = false;
                        break;
                }
            }
        }
    }

    @Override
    protected void Move(boolean is_Right ,boolean Pressed) {
        if(stun > 0){ return;}
        if(before_Right != is_Right){
            canDash = 0;
        }
        if(!is_Dash){
            boolean goAhead;
            dx = ((goAhead = is_Right ^ look!=Looking.Right) ? 7f : 4.5f) * (is_Right ? 1f : -1f);
            System.out.println(walkCount);
            if (!is_Jump) {
            NowImageName = (walkCount = ((walkCount + 1) % 20)) / 10 == 0 ? "Stand" : "Walk";
            }

            if(goAhead && Pressed && !is_Jump && !bK_DOWN){
                if(canDash > 0){is_Dash = true; NowImageName = "Run";}
                canDash = 7;
            }
        }else{
            dx = is_Right ? 20f : -20f;
        }

        before_Right = is_Right;
    }

    @Override
    public void Action() {
        super.Action();
    }

    //最初に画像登録を行う
    @Override
    public void RegisterImage() {
        NowImageName = "Stand";
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Images.put("Stand", toolkit.getImage(getClass().getResource("resources/Nao_stand.png")));
        Images.put("Punch1", toolkit.getImage(getClass().getResource("resources/Nao_punch1.png")));
        Images.put("Punch2", toolkit.getImage(getClass().getResource("resources/Nao_punch2.png")));
        Images.put("Kick3", toolkit.getImage(getClass().getResource("resources/Nao_Kick3.png")));
        Images.put("Run", toolkit.getImage(getClass().getResource("resources/Nao_FastMove.png")));
        Images.put("Walk", toolkit.getImage(getClass().getResource("resources/Nao_walk.png")));
        Images.put("Guard", toolkit.getImage(getClass().getResource("resources/Nao_guard.png")));
        Images.put("Damage", toolkit.getImage(getClass().getResource("resources/Nao_ukete1.png")));
        Images.put("Down", toolkit.getImage(getClass().getResource("resources/Nao_down.png")));
        Images.put("Jump", toolkit.getImage(getClass().getResource("resources/Nao_jump.png")));
        Images.put("Win", toolkit.getImage(getClass().getResource("resources/Nao_win.png")));
        Images.put("FlyingKick", toolkit.getImage(getClass().getResource("resources/Nao_flyingattack.png")));
        Images.put("StrongPunch", toolkit.getImage(getClass().getResource("resources/Nao_strong.png")));
    }

    @Override
    public void RegisterAudioClip(TreeMap<String, AudioClip> audios) {
        audios.put("Nao_punch1", java.applet.Applet.newAudioClip(getClass().getResource("resources/Nao_voice1.wav")));
        audios.put("Nao_punch2", java.applet.Applet.newAudioClip(getClass().getResource("resources/Nao_voice2.wav")));
        audios.put("Nao_kick3", java.applet.Applet.newAudioClip(getClass().getResource("resources/Nao_voice3.wav")));
        audios.put("Nao_win", java.applet.Applet.newAudioClip(getClass().getResource("resources/Nao_win.wav")));
    }

    @Override
    public void GameFinished(int WinOrLoseOrDraw, boolean HPeqZero) {
        switch (WinOrLoseOrDraw){
            case 0://Win
                NowImageName = "Win";
                if(!did_PlayWin){
                    NowRequestedPlayAudios.add("Nao_win");
                    did_PlayWin = true;
                }
                break;
            case 1://Lose

                NowImageName = HPeqZero ? "Down" : "Damage";
                break;
            case 2://Draw
                NowImageName = "FlyingKick";
                break;

        }
    }

}

//操作キャラ
class Shunchan extends UECPlayerBase{

    public Shunchan(int K_UP, int K_DOWN, int K_LEFT, int K_RIGHT, int K_WeakAttack, int K_StrongAttack, float positionX, float positionY, float magnification, Point size, Point range, Point startRange, boolean is_Right){
        super(K_UP, K_DOWN, K_LEFT, K_RIGHT, K_WeakAttack, K_StrongAttack, positionX, magnification, size, range, startRange, is_Right);
        HP = 120;//体力セット
    }

    @Override
    protected void Jump(boolean Pressed) {
        super.Jump(Pressed);
    }

    @Override
    protected void Squat(boolean Pressed) {
        super.Squat(Pressed);
    }

    @Override
    public boolean ConfirmDamage(Point2D.Float Force, int Stun, int damage, boolean canDown){
        if(!canBlock){NowRequestedPlayAudios.add("Kiu_damage");}
        return super.ConfirmDamage(Force, Stun, damage, canDown);
    }


    @Override
    protected void WeakAttack(boolean Pressed) {
        /*
        attackInfo.setInfo(
        攻撃範囲の始点[右向き基準],
        攻撃範囲,
        int Occurrence… 攻撃発生までの時間,
        int Duration… 攻撃の継続時間,
        int Interval… 次の攻撃入力を受け付けない時間,
        int ContinuousHit… 多段ヒット攻撃ならその間隔をセットする[継続時間中は指定時間経過で攻撃判定復活],
        int Damage… ダメージ,
        Point2D.Float Force… 攻撃時に相手を吹っ飛ばす向き[右向き基準],
        int stun… 攻撃を受けてひるむ時間,
        int id… 攻撃のid)
         */
        if(canAttack) {
            if (Pressed) {
                if(!is_Jump) {
                    //地上技
                    dx = 0f;
                    switch (attackId) {
                        default:
                            NowImageName = "Punch1";
                            NowRequestedPlayAudios.add("Kiu_attack1");
                            attackInfo.setInfo(new Point(80, 40), new Point(20, 20), 0, 5, 10, 9999, 5, new Point2D.Float(3f, 0f), 2, 1, false);
                            attackId = 1;
                            canCombo = 7;
                            canAttack = false;
                            break;
                        case 1:
                            NowImageName = "Kick3";
                            NowRequestedPlayAudios.add("Kiu_attack3");
                            NowRequestedPlayAudios.add("Kiu_beam");
                            attackInfo.setInfo(new Point(100, 40), new Point(50, 10), 0, 20, 30, 2, 4, new Point2D.Float(10f, -10f), 10, -1, false);
                            attackId = -1;
                            canCombo = 0;
                            canAttack = false;
                            break;
                    }
                }else{
                    //空中技
                    switch (attackId) {
                        default:
                            NowImageName = "Punch2";
                            NowRequestedPlayAudios.add("Kiu_attack2");
                            attackInfo.setInfo(new Point(80, 0), new Point(20, 30), 5, 10, 5, 9999, 10, new Point2D.Float(20f, -2f), 5, 2, false);
                            attackId = -1;
                            canCombo = 0;
                            canAttack = false;
                            break;
                    }
                }
            }
        }
    }

    @Override
    protected void StrongAttack(boolean Pressed) {
        if(canAttack) {
            if (Pressed) {

            }
        }
    }

    @Override
    protected void Move(boolean is_Right ,boolean Pressed) {
        if(stun > 0){ return;}
        if(before_Right != is_Right){
            canDash = 0;
        }
        if(!is_Dash){
            boolean goAhead;
            dx = ((goAhead = is_Right ^ look!=Looking.Right) ? 5f : 3.5f) * (is_Right ? 1f : -1f);
            System.out.println(walkCount);
            if (!is_Jump) {
                NowImageName = (walkCount = ((walkCount + 1) % 20)) / 10 == 0 ? "Stand" : "Walk";
            }

            if(goAhead && Pressed && !is_Jump && !bK_DOWN){
                if(canDash > 0){is_Dash = true; NowImageName = "Run";}
                canDash = 7;
            }
        }else{
            dx = is_Right ? 25f : -25f;
        }

        before_Right = is_Right;
    }

    @Override
    public void Action() {
        super.Action();
    }

    //最初に画像登録を行う
    @Override
    public void RegisterImage() {
        NowImageName = "Stand";
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Images.put("Stand", toolkit.getImage(getClass().getResource("resources/Kiu_stand.png")));
        Images.put("Punch1", toolkit.getImage(getClass().getResource("resources/Kiu_attack1.png")));
        Images.put("Punch2", toolkit.getImage(getClass().getResource("resources/Kiu_attack2.png")));
        Images.put("Kick3", toolkit.getImage(getClass().getResource("resources/Kiu_attack3.png")));
        Images.put("Run", toolkit.getImage(getClass().getResource("resources/Kiu_FastMove.png")));
        Images.put("Walk", toolkit.getImage(getClass().getResource("resources/Kiu_walk.png")));
        Images.put("Guard", toolkit.getImage(getClass().getResource("resources/Kiu_guard.png")));
        Images.put("Damage", toolkit.getImage(getClass().getResource("resources/Kiu_ukemi.png")));
        Images.put("Down", toolkit.getImage(getClass().getResource("resources/Kiu_down.png")));
        Images.put("Jump", toolkit.getImage(getClass().getResource("resources/Kiu_jump.png")));
    }

    @Override
    public void RegisterAudioClip(TreeMap<String, AudioClip> audios) {
        audios.put("Kiu_attack1", java.applet.Applet.newAudioClip(getClass().getResource("resources/Kiu_attack1.wav")));
        audios.put("Kiu_attack2", java.applet.Applet.newAudioClip(getClass().getResource("resources/Kiu_attack2.wav")));
        audios.put("Kiu_attack3", java.applet.Applet.newAudioClip(getClass().getResource("resources/Kiu_attack3.wav")));
        audios.put("Kiu_damage", java.applet.Applet.newAudioClip(getClass().getResource("resources/Kiu_guard.wav")));
        audios.put("Kiu_win", java.applet.Applet.newAudioClip(getClass().getResource("resources/Kiu_win.wav")));
        audios.put("Kiu_beam", java.applet.Applet.newAudioClip(getClass().getResource("resources/Nao_voice.wav")));
    }

    @Override
    public void GameFinished(int WinOrLoseOrDraw, boolean HPeqZero) {
        switch (WinOrLoseOrDraw){
            case 0://Win
                NowImageName = "Jump";
                if(!did_PlayWin){
                    NowRequestedPlayAudios.add("Kiu_win");
                    did_PlayWin = true;
                }

                break;
            case 1://Lose
                NowImageName = HPeqZero ? "Down" : "Damage";
                break;
            case 2://Draw
                NowImageName = "Run";
                break;

        }
    }
}

//方向
enum Looking{
    Right(1f),
    Left(-1f);

    private final float value;

    Looking(final float value){
        this.value = value;
    }

    public float getValue(){
        return value;
    }
}

//攻撃情報管理クラス、攻撃のたびに使い回し
class AttackInfo{
    private Point StartingPoint, RangePoint;//キャラ座標を基準とする攻撃の始点終点(右基準)
    private int sOccurrence, sDuration, sInterval, NowFrame = 0;//攻撃判定が始まる時間, 攻撃継続時間, 攻撃後入力を受け付けない時間 (基本的にはDuration >= Interval、モーションの時間とも一致させたら楽だと思う), 現在の経過時間
    private int Damage, stun;//ダメージ量
    private int id, mode;//攻撃ID, 攻撃の状態(updateメソッド参照)
    private boolean alreadyHit = false;//既に攻撃判定を終えているか
    private float magnification;
    private Point OwnSize, OpponentSize, OpponentRange, OpponentStartRange;//互いのプレイヤーの大きさ, 相手の当たり判定のサイズ, 相手の当たり判定の開始位置(右基準)
    private boolean isHaving = false;//攻撃情報を所持してい
    private boolean canDown;//攻撃ヒット時ダウンするかどうか
    private int ContinuousHit, NowContinuousHit;//多段ヒット攻撃の時、次弾ヒットまでの時間を登録。単発攻撃なら9999をセットする。
    private Point2D.Float Force;//攻撃時の加力ベクトル
    private Point2D.Float OwnPos;//自身の座標
    private UECPlayerBase Own, Opponent;
    private Looking look;//右向いているか否か



    public AttackInfo(UECPlayerBase Own, UECPlayerBase Opponent, float magnification){
        this.OwnSize = Own.getMySize();
        this.Own = Own;
        this.Opponent = Opponent;
        this.OpponentSize = Opponent.getMySize();
        this.OpponentRange = Opponent.getRange();
        this.OpponentStartRange = Opponent.getStartRange();
        this.magnification = magnification;
    }

    public void setInfo(Point StartingPoint, Point RangePoint, int Occurrence,  int Duration, int Interval,int ContinuousHit, int Damage, Point2D.Float Force, int stun, int id, boolean canDown){
        this.StartingPoint = new Point((int)(StartingPoint.x * magnification), (int)(StartingPoint.y * magnification) );
        this.RangePoint = new Point((int)(RangePoint.x * magnification), (int)(RangePoint.y * magnification) );;
        this.sOccurrence = Occurrence;
        this.sDuration = Duration + Occurrence;
        this.sInterval = Interval + Duration + Occurrence;
        this.ContinuousHit = this.NowContinuousHit = ContinuousHit;
        this.Damage = Damage;
        this.Force = Force;
        this.stun = stun;
        this.id = id;
        this.canDown = canDown;
        NowFrame = 0;
        isHaving = true;
        alreadyHit = false;
    }

    //アクセサメソッド
    public int getDamage(){ return Damage; }
    public void setHaving(boolean isHaving){ this.isHaving = isHaving; }
    public boolean isHaving(){ return isHaving; }

    //継続時間を管理する。現在の状態を数値として返す
    public int update(){
        NowFrame++;
        if(NowFrame > sInterval){
            isHaving = false;
            Own.setNowImageName("Stand");
            return mode = 3; //AfterInterval
        }else if(NowFrame > sDuration){
            return mode = 2; //Interval
        }else if(NowFrame > sOccurrence){
            return mode = 1; //Attacking
        }else{
            return mode = 0; //BeforeAttacking
        }
    }

    //当たり判定の確認を行う
    public boolean judgeHitted(Looking look, Point2D.Float OwnPos, Point2D.Float OpponentPos){
        //既に攻撃はヒットした
        if (alreadyHit){
            if (NowContinuousHit-- > 0){
                return false;
            }else{
                //次弾までの時間が過ぎた
                NowContinuousHit = ContinuousHit;
                alreadyHit = false;
            }
        }
        //相手はダウン中で攻撃できない
        if (Opponent.getCanDown()){ return false; }


        //自身の向きを引数に。向きが左ならば全てを反転して右向きとして考える。
        this.look = look;
        this.OwnPos = OwnPos;
        Point2D.Float rightOwnPos, rightOpponentPos;
        if(look == Looking.Right){
            rightOwnPos = OwnPos;
            rightOpponentPos = OpponentPos;
        }else{
            rightOwnPos = new Point2D.Float(UECFighter.SCREEN_WIDTH - OwnPos.x - OwnSize.x, OwnPos.y);
            rightOpponentPos = new Point2D.Float(UECFighter.SCREEN_WIDTH - OpponentPos.x - OpponentSize.x, OpponentPos.y);
        }

        boolean isHitted = rightOwnPos.x + StartingPoint.x + RangePoint.x >= rightOpponentPos.x && rightOwnPos.x + StartingPoint.x <= rightOpponentPos.x + OpponentSize.y &&
                rightOwnPos.y + StartingPoint.y + RangePoint.y >= rightOpponentPos.y && rightOwnPos.y + StartingPoint.y <= rightOpponentPos.y + OpponentSize.y;

        alreadyHit = isHitted;
        return isHitted;
    }

    public void ConfirmAttack(){
        System.out.println(Force != null);
        Opponent.ConfirmDamage(new Point2D.Float(Force.x * look.getValue(), Force.y), stun, Damage, canDown);
    }

    //デバッグ用攻撃範囲描画(右向き描画のみ対応)
    public void print(Graphics g, Point2D.Float OwnPos){
        if(isHaving){
            switch (mode){
                case 0:
                    g.setColor(Color.BLUE);
                    break;
                case 1:
                    g.setColor(Color.RED);
                    break;
                case 2:
                    g.setColor(Color.ORANGE);
                    break;
                case 3:
                    g.setColor(Color.GREEN);
                    break;
            }

            g.fillRect((int) OwnPos.x + StartingPoint.x, (int) OwnPos.y + StartingPoint.y, RangePoint.x, RangePoint.y);

        }
    }
}

//-------------------------------------------------------画面構成&ゲーム管理クラス

//スタート画面
class StartFrameView extends JPanel implements ActionListener{
    private UECFighter uecFighter;
    private String[] argv = new String[4];
    private int c_point, select_time;
    private Image title, cursole_fist_red, cursole_fist_blue, backImage;
    private javax.swing.Timer select; //ENTER押した後にちょっとだけ待つ用
    private Font font;
    private boolean flash;
    private TreeMap<String, AudioClip> audios;

    public StartFrameView(UECFighter uecFighter){
        this.uecFighter = uecFighter;
        this.setSize(UECFighter.SCREEN_WIDTH, UECFighter.HEIGHT);
        this.setBackground(Color.white);
        this.setLayout(null);
        setOpaque(false);
        c_point = 250;
        select = new javax.swing.Timer(1000, this);
        select_time = 1;
        audios = new TreeMap<String, AudioClip>();

        //データの登録
        RegisterAudioClip();
        title = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/uec_fighter.png"));
        cursole_fist_red = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/fire_red.png"));
        cursole_fist_blue = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/fire_blue.png"));
        backImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/uec_back.jpg"));
        this.loadFont("resources/V-GERB(bold).ttf");

        this.repaint();
    }

    //@Override
    public void keyTyped(KeyEvent e){

    }

    //@Override
    public void keyPressed(KeyEvent e){
        int keycode = e.getKeyCode();
        switch(keycode){
            case KeyEvent.VK_UP:
                if(c_point > 250){
                     c_point -= 80;
                     PlaySoundEffect("Select");
                }
                this.repaint();
                break;
            case KeyEvent.VK_DOWN:
                if(c_point < 330) {
                    c_point += 80;
                    PlaySoundEffect("Select");
                }
                this.repaint();
                break;
            case KeyEvent.VK_ENTER:
                if(c_point == 250){
                    PlaySoundEffect("Decision");
                    select.start();
                }else{
                    uecFighter.callScene(3);
                }
                break;
        }
    }

    //@Override
    public void keyReleased(KeyEvent e){

    }

    public void paint(Graphics g){
        g.drawImage(backImage, 0, 0, UECFighter.SCREEN_WIDTH, UECFighter.SCREEN_HEIGHT, this);
        super.paint(g);
        g.drawImage(title, 60, 20, 600, 233, this);
        g.drawImage(cursole_fist_red, 170, c_point, 80, 50, this);
        g.drawImage(cursole_fist_blue, 450, c_point, 80, 50, this);
        g.setFont(font);
        g.setColor(Color.black);
        g.drawString("START", 270, 300);
        g.drawString("OPTION", 260, 380);
    }

    public void loadFont(String filename){
        try {
            InputStream is = getClass().getResourceAsStream(filename);
            font = Font.createFont(Font.TRUETYPE_FONT, is);
            font = font.deriveFont(50.0f);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }catch (FontFormatException ffe){
            ffe.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent e){
        select_time--;
        if(select_time == 0){
            select.stop();
            uecFighter.callScene(1);
        }
    }

    private void RegisterAudioClip(){
        audios.put("Select", java.applet.Applet.newAudioClip(getClass().getResource("resources/cursor_move.wav")));
        audios.put("Decision", java.applet.Applet.newAudioClip(getClass().getResource("resources/decision.wav")));
    }

    private void PlaySoundEffect(String soundName){
        AudioClip audioClip = audios.get(soundName);
        audioClip.play();
    }
}

//キャラ選択画面
class PlayerSelect extends JPanel {

    /*キャラ選択実装で必要なもの...
        選択されているかどうかを判定、選択をキャンセル、試合に進めるかどうかを判断するもの*/

    private UECFighter uecFighter;
    private JLabel player1, player2, p_1, p_2, OK_1, OK_2;    //player
    //private JButton p_1, p_2;
    private int p_enabled[], p_num,
                p1_right, p1_left, p1_up, p1_down, p1_OK, p1_cancel,
                p2_right, p2_left, p2_up, p2_down, p2_OK, p2_cancel;
    private boolean p_selected[]; //選択されているかどうかを判定 1P...selected[0], 2P...selected[1]
    private Image img[], backImage, img_kiu, img_nao, img_1p, img_2p;
    private Font font, font_name; //fontをいじる際に使用
    private TreeMap<String, AudioClip> audios; //音声
    private AudioClip bgm;

    public PlayerSelect(UECFighter uecFighter){
        this.uecFighter = uecFighter;
        p_num = 2;
        p_enabled = new int[p_num];
        img = new Image[2];
        p_selected = new boolean[p_num];
        for(int i=0; i<p_num; i++){
            p_selected[i] = false;
            p_enabled[i] = 0; //0がKIU, 1がNAO
        }
        p1_right = KeyEvent.VK_D; p1_left = KeyEvent.VK_A; //p1_up = KeyEvent.VK_W; p1_down = KeyEvent.VK_S;
        p1_OK = KeyEvent.VK_C; p1_cancel = KeyEvent.VK_V;
        p2_right = KeyEvent.VK_L; p2_left = KeyEvent.VK_J; //p2_up = KeyEvent.VK_I; p2_down = KeyEvent.VK_K;
        p2_OK = KeyEvent.VK_COMMA; p2_cancel = KeyEvent.VK_PERIOD;
        this.setSize(UECFighter.SCREEN_WIDTH, UECFighter.SCREEN_HEIGHT);
        setOpaque(false); //背景を透明に(これをしないと背景に画像貼れない)

        //bgm再生
        bgm = java.applet.Applet.newAudioClip(getClass().getResource("resources/sentaku_bgm2.wav"));
        bgm.loop();

        //データの登録
        backImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/china_back.jpg"));
        img_1p = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/1P.png"));
        img_2p = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/2P.png"));
        img_kiu = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/Kiu_sentaku.png"));
        img_nao = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/Nao_sentaku.png"));
        img[0] = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/Kiu_walk.png"));
        img[1] = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/Nao_stand.png"));
        font = loadFont("resources/V-GERB(bold).ttf", 50.0f);
        font_name = loadFont("resources/V-GERB(bold).ttf", 15.0f);
        audios = new TreeMap<String, AudioClip>();
        RegisterAudioClip();
    }

    public void keyTyped(KeyEvent e){

    }

    public void keyPressed(KeyEvent e){
        int key = e.getKeyCode();
        if(!p_selected[0]){
            if(key == p1_right){
                if(p_enabled[0]<1){
                    p_enabled[0] += 1;
                }
            }if(key == p1_left){
                if(p_enabled[0]>0){
                    p_enabled[0] -= 1;
                }
            }
        }if(!p_selected[1]){
            if(key == p2_right){
                if(p_enabled[1]<1){
                    p_enabled[1] += 1;
                }
            }if(key == p2_left){
                if(p_enabled[1]>0){
                    p_enabled[1] -= 1;
                }
            }
        }if(key == p1_OK){
            PlaySoundEffect("Decision");
            if(p_enabled[0] == 0){
                PlaySoundEffect("Kiu_decision");
            }else {
                PlaySoundEffect("Nao_decision");
            }
            p_selected[0] = true;
        }if(key == p2_OK){
            PlaySoundEffect("Decision");
            if(p_enabled[1] == 0){
                PlaySoundEffect("Kiu_decision");
            }else {
                PlaySoundEffect("Nao_decision");
            }
            p_selected[1] = true;
        }if(key == p1_cancel){
            p_selected[0] = false;
        }if(key == p2_cancel){
            p_selected[1] = false;
        }if(key == KeyEvent.VK_ENTER){
            if(p_selected[0] && p_selected[1]){
                PlaySoundEffect("START");
                bgm.stop(); bgm = null;
                uecFighter.setPlayer(p_enabled[0], p_enabled[1]);
                uecFighter.callScene(2);
            }
        }
        repaint();
    }

    public void keyReleased(KeyEvent e){

    }

    @Override
    public void paint(Graphics g){
        g.drawImage(backImage, 0, 0, UECFighter.SCREEN_WIDTH, UECFighter.SCREEN_HEIGHT, this);
        super.paint(g);

        g.setFont(font);
        g.setColor(Color.white);
        g.drawString("PLAYER SELECT", 170, 50);
        g.setColor(new Color(255, 255, 255, 50));
        g.fillOval(100, 70, 200, 300);
        g.fillOval(420, 70, 200, 300);

        //player's Icon
        g.drawImage(img_kiu, 270, 400, 80, 80, this);
        g.drawImage(img_nao, 370, 400, 80, 80, this);
        g.setFont(font_name);
        g.drawString("SHUNCHAN", 270, 500);
        g.drawString("NAOCHAN", 380, 500);

        //カーソル
        g.drawImage(img_1p, 150, 320, 100, 50, this);
        g.drawImage(img_2p, 470, 320, 100, 50, this);
        for(int i=0; i<p_num; i++){
            g.drawImage(img_1p, 270+(p_enabled[0])*100-22, 398, 20, 10, this);
            g.drawImage(img_2p, 270+(p_enabled[1])*100-22, 410, 20, 10, this);

            g.setColor(Color.red);
            g.drawRect(270+(p_enabled[0])*100-2, 398, 82, 82);
            g.setColor(Color.blue);
            g.drawRect(270+(p_enabled[1])*100-2, 398, 82, 82);
        }

        //選択された後の描画
        g.drawImage(img[p_enabled[0]], 50, 60, 300, 300, this);
        g.drawImage(img[p_enabled[1]], 670, 60, -300, 300, this);
        if(p_selected[0]){
            g.setFont(font);
            g.setColor(Color.red);
            g.drawString("OK!", 50, 370);
        }
        if(p_selected[1]){
            g.setFont(font);
            g.setColor(Color.blue);
            g.drawString("OK!", 600, 370);
        }

        //決定
        if(p_selected[0] && p_selected[1]){
            g.setColor(Color.black);
            g.fillRect(0, 190, UECFighter.SCREEN_WIDTH, 60);
            g.setColor(new Color(255, 255, 0));
            g.setFont(font);
            g.drawString("START!", 200, 240);
            g.setFont(font_name);
            g.drawString("PRESS ENTER", 400, 240);
        }
    }

    public Font loadFont(String filename, float size){
        try {
            InputStream is = getClass().getResourceAsStream(filename);
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            font = font.deriveFont(size);
            return font;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }catch (FontFormatException ffe){
            ffe.printStackTrace();
        }
        return font;
    }

    private void RegisterAudioClip(){
        audios.put("Select", java.applet.Applet.newAudioClip(getClass().getResource("resources/slap2.wav")));
        audios.put("Kiu_decision", java.applet.Applet.newAudioClip(getClass().getResource("resources/Kiu_sentaku.wav")));
        audios.put("Nao_decision", java.applet.Applet.newAudioClip(getClass().getResource("resources/Nao_sentaku.wav")));
        audios.put("Decision", java.applet.Applet.newAudioClip(getClass().getResource("resources/decision.wav")));
        audios.put("START", java.applet.Applet.newAudioClip(getClass().getResource("resources/kick3.wav")));
    }

    private void PlaySoundEffect(String soundName){
        AudioClip audioClip = audios.get(soundName);
        audioClip.play();
    }
}

//試合時間を管理
class GameTime implements ActionListener{
    private javax.swing.Timer gameTime;
    private int starttime, time;

    public GameTime(int time){
        this.time = time;
        starttime = 4;
        gameTime = new javax.swing.Timer(1000, this);
        gameTime.start();
    }

    public int getTime(){ return time; }

    public int getstart(){ return starttime;}

    public void setTime(){
        if(starttime > 0){
            starttime--;
        }
        if(starttime == 0 && time>0){
            time--;
        }
    }

    public void actionPerformed(ActionEvent e){
        this.setTime();
    }

    public void stop(){
        gameTime.stop();
    }
}

//試合画面
class UECFrameView extends JPanel {//implements KeyListener{
    private java.util.Timer gameThread;//ゲーム用スレッド
    private GameTime gameTime;
    private JLabel timeLabel, starttime;
    private int scene, time; //ゲームのシーンをmanegementする為の変数
    private boolean canOperate;

    private UECPlayerBase player1, player2;
    private Point2D.Float p1position, p2position;
    private Point p1size, p2size, p1range, p2range, p1startRange, p2startRange;
    private Image p1image, p2image, p1here, p2here, timerFrame, backImage;
    private AttackInfo p1AttackInfo, p2AttackInfo;
    private Font font, font_time;
    private MediaTracker tracker;

    private TreeMap<String, AudioClip> audios;
    private AudioClip bgm;

    //コンストラクタの引数 P1,P2はKiu,Naoのどちらを描画するか決めるために使う。0がKiu,1がNao
    public UECFrameView(UECFighter uecFighter, int P1, int P2, int time){
        this.setSize(UECFighter.SCREEN_WIDTH, UECFighter.HEIGHT);
        this.setBackground(new Color(150,255,255));
        this.setLayout(null);
        setOpaque(false);
        canOperate = false;

        timerFrame = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/timer_frame.png"));

        //プレイヤー1
        player1 = getPlayerData(P1, true);
        p1size = player1.getMySize();
        p1range = player1.getRange();
        p1startRange = player1.getStartRange();
        //プレイヤー2
        player2 = getPlayerData(P2, false);
        p2size = player2.getMySize();
        p2range = player2.getRange();
        p2startRange = player2.getStartRange();

        //互いの対戦相手の情報を交換する
        player1.setOpponentPlayer(player2);
        player2.setOpponentPlayer(player1);
        //攻撃情報管理クラス(AttackInfo)を準備する
        player1.setAttackInfo(player2);
        player2.setAttackInfo(player1);
        //攻撃を扱う
        p1AttackInfo = player1.getAttackInfo();
        p2AttackInfo = player2.getAttackInfo();
        //p1here,p2here準備
        p1here = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/1P.png"));
        p2here = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/2P.png"));
        backImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/d_tou.jpg"));


        //効果音登録
        audios = new TreeMap<String, AudioClip>();
        RegisterAudioClip();
        //BGM再生
        bgm = java.applet.Applet.newAudioClip(getClass().getResource("resources/fight_bgm.wav"));
        bgm.loop();

        gameTime = new GameTime(time);
        font = new Font(Font.SANS_SERIF,Font.BOLD, 80);

        font_time = new Font(Font.SANS_SERIF, JLabel.CENTER, 32);
        /*timeLabel = new JLabel(Integer.toString(gameTime.getTime()), JLabel.CENTER);
        timeLabel.setBackground(Color.WHITE);
        timeLabel.setBounds(320, 0, 80, 80);
        timeLabel.setFont(font);
        timeLabel.setOpaque(true);
        this.add(timeLabel);*/

        //デバッグ用
        player1.setDebugmessage(false);
        player2.setDebugmessage(true);

        //
        gameThread = new java.util.Timer();
        gameThread.schedule(new TimerTask() {
            @Override
            public void run() {
                player1.Action();
                player2.Action();

                p1position = player1.getPosition();
                p2position = player2.getPosition();

                boolean p2Hitted = false, p1Hitted = false;
                int p1mode, p2mode;
                //"AttackInfo.update()==1" means "Attacking".
                if(p1AttackInfo.isHaving()){
                    switch (p1mode = p1AttackInfo.update()){
                        case 1:
                            p2Hitted = p1AttackInfo.judgeHitted(player1.getLook(), p1position, p2position);
                            break;
                        case 3:
                            player1.setCanAttack(true);
                            break;
                    }

                }
                if(p2AttackInfo.isHaving()){
                    switch (p2mode = p2AttackInfo.update()){
                        case 1:
                            p1Hitted = p2AttackInfo.judgeHitted(player2.getLook(), p2position, p1position);
                            break;

                        case 3:
                            player2.setCanAttack(true);
                            break;
                    }

                }

                if(p2Hitted){
                    p1AttackInfo.ConfirmAttack();
                    p2AttackInfo.setHaving(false);
                }
                if(p1Hitted){
                    p2AttackInfo.ConfirmAttack();
                    p1AttackInfo.setHaving(false);
                }

                repaint();
            }
        }, 0, 20);
    }

    @Override
    public void paint(Graphics g) {
        //背景表示
        g.drawImage(backImage, 0, 0, UECFighter.SCREEN_WIDTH, UECFighter.SCREEN_HEIGHT, this);

        super.paint(g);

        /*当たり判定デバッグ用
        p1AttackInfo.print(g, p1position);
        p2AttackInfo.print(g, p2position);
        */

        //fillRectでHPを表示してます。
        g.setColor(new Color(0, 200, 0));
        g.fillRect(320 - player1.getHP() * 3, 30, player1.getHP() * 3, 20);
        g.fillRect(400, 30, player2.getHP() * 3, 20);

        //SE再生
        PlaySoundEffect(player1.getNowRequestedPlayAudio());
        PlaySoundEffect(player2.getNowRequestedPlayAudio());

        //timeLabel.setText(Integer.toString(gameTime.getTime()));
        g.drawImage(timerFrame, 300, -20, 120, 120, this);
        g.setFont(font_time);
        g.setColor(Color.black);
        g.drawString(Integer.toString(gameTime.getTime()), 328, 50);
        g.setFont(font);
        g.setColor(Color.red);
        if (gameTime.getstart() == 0) {
            canOperate = true;
        }
        if (gameTime.getTime() == 0) {
            canOperate = false;
            if (player1.getHP() > player2.getHP()) {
                g.drawString("1P WIN!!", 220, 240);
                player1.GameFinished(0, false); player2.GameFinished(1, false);
            } else if (player1.getHP() < player2.getHP()) {
                g.drawString("2P WIN!!", 220, 240);
                player1.GameFinished(1, false); player2.GameFinished(0, false);
            } else {
                g.drawString("DRAW!!", 250, 240);
                player1.GameFinished(2, false); player2.GameFinished(2, false);
            }
        }
        switch (gameTime.getstart()) {
            case 2:
                g.drawString("Ready", 260, 240);
                break;
            case 1:
                g.drawString("GO!", 300, 240);
                break;
        }
        if (player1.getHP() <= 0) {
            g.drawString("2P WIN!!", 220, 240);
            player1.GameFinished(1, true); player2.GameFinished(0, true);
            gameTime.stop();
            canOperate = false;
        } else if (player2.getHP() <= 0) {
            g.drawString("1P WIN!!", 220, 240);
            player1.GameFinished(0, true); player2.GameFinished(1, true);
            gameTime.stop();
            canOperate = false;
        }

        //位置を渡してついでにJump判定もやってもらうことにした
        if ((p1image = player1.getNowImage()) != null) {
            //(p1 < p2) -> lookingRight
            if (player1.setgetLook(p2position) == Looking.Right) {
                g.drawImage(p1image,
                        (int) p1position.x, (int) p1position.y,
                        p1size.x, p1size.y, null);
                g.drawImage(p1here,
                        (int) p1position.x + p1size.x / 2 - 25, UECFighter.SCREEN_HEIGHT - 70, 60, 30, null);
            } else {
                g.drawImage(p1image,
                        (int) p1position.x + p1size.x, (int) p1position.y,
                        -p1size.x, p1size.y, null);
                g.drawImage(p1here,
                        (int) p1position.x + p1size.x / 2 - 25, UECFighter.SCREEN_HEIGHT - 70, 60, 30, null);
            }
        }
        if ((p2image = player2.getNowImage()) != null) {
            //p2 < p1 -> lookingRight
            if (player2.setgetLook(p1position) == Looking.Right) {
                g.drawImage(p2image,
                        (int) p2position.x, (int) p2position.y,
                        p2size.x, p2size.y, null);
                g.drawImage(p2here,
                        (int) p2position.x + p2size.x / 2 - 25, UECFighter.SCREEN_HEIGHT - 70, 60, 30, null);
            } else {
                g.drawImage(p2image,
                        (int) p2position.x + p2size.x, (int) p2position.y,
                        -p2size.x, p2size.y, null);
                g.drawImage(p2here,
                        (int) p2position.x + p2size.x / 2 - 25, UECFighter.SCREEN_HEIGHT - 70, 60, 30, null);
            }
        }
    }

    //@Override
    public void keyTyped(KeyEvent e) {
        requestFocusInWindow();
    }

    //@Override
    public void keyPressed(KeyEvent e) {
        if(canOperate){
            player1.keyPressed(e);
            player2.keyPressed(e);
        }
    }

    //@Override
    public void keyReleased(KeyEvent e) {
        if(canOperate){
            player1.keyReleased(e);
            player2.keyReleased(e);
        }
    }

    private void RegisterAudioClip(){
        //共有効果音
        audios.put("Punch", java.applet.Applet.newAudioClip(getClass().getResource("resources/punch1.wav")));
        audios.put("guard", java.applet.Applet.newAudioClip(getClass().getResource("resources/Nao_guard.wav")));
        //キャラボイス
        player1.RegisterAudioClip(audios);
        player2.RegisterAudioClip(audios);
    }

    private void PlaySoundEffect(List<String> reqestedSoundNames){
        while (!reqestedSoundNames.isEmpty()){
            String soundName = reqestedSoundNames.remove(0);
            AudioClip audioClip = audios.get(soundName);
            audioClip.play();
        }
    }



    //キャラのデータを返す
    private UECPlayerBase getPlayerData(int CharId, boolean isP1){
        UECPlayerBase chosenChar = null;
        Point size, range, startRange;
        int ButtonId[];

        if (isP1){
            ButtonId = new int[]{KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,  KeyEvent.VK_C, KeyEvent.VK_V};
        }else{
            ButtonId = new int[]{KeyEvent.VK_I, KeyEvent.VK_K, KeyEvent.VK_J, KeyEvent.VK_L,  KeyEvent.VK_COMMA, KeyEvent.VK_PERIOD};
        }
        switch (CharId){
            case 0://Kiuchi
                float p2magnification = 2.2f;
                size = new Point((int) (120 * p2magnification), (int) (120 * p2magnification));
                range = new Point((int) (30 * p2magnification), (int) (70 * p2magnification));
                startRange = new Point((int) (45 * p2magnification), (int) (50 * p2magnification));
                chosenChar = new Shunchan(ButtonId[0], ButtonId[1], ButtonId[2], ButtonId[3],  ButtonId[4], ButtonId[5], isP1 ? 0 : UECFighter.SCREEN_WIDTH-size.x, UECFighter.SCREEN_HEIGHT-size.y, p2magnification, size, range, startRange, false);

                break;
            case 1://Naochan
                float p1magnification = 2f;
                size = new Point((int) (120 * p1magnification), (int) (120 * p1magnification));
                range = new Point((int) (30 * p1magnification), (int) (70 * p1magnification));
                startRange = new Point((int) (45 * p1magnification), (int) (50 * p1magnification));
                chosenChar = new NaoChan(ButtonId[0], ButtonId[1], ButtonId[2], ButtonId[3],  ButtonId[4], ButtonId[5], isP1 ? 0 : UECFighter.SCREEN_WIDTH-size.x, UECFighter.SCREEN_HEIGHT-size.y, p1magnification, size, range, startRange, true);
                break;

        }

        return chosenChar;
    }

}

//オプしション画面
class Option extends JPanel {
    private UECFighter uecFighter;
    private Font font, option;
    private Image backImage, change, fist;
    private int enabled, cursor, time;

    public Option(UECFighter uecFighter){
        this.uecFighter = uecFighter;
        font = loadFont("resources/V-GERB(bold).ttf", 50.0f);
        option = loadFont("resources/V-GERB(bold).ttf", 30.0f);
        cursor = 0;
        enabled = 50;
        time = 120;
        backImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/china_back.jpg"));
        fist = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/fire_red.png"));
        change = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/cursor1.png"));
        setOpaque(false);
    }

    @Override
    public void paint(Graphics g){
        g.drawImage(backImage, 0, 0, UECFighter.SCREEN_WIDTH, UECFighter.SCREEN_HEIGHT, this);
        super.paint(g);
        g.setColor(Color.white);
        g.setFont(font);
        g.drawString("OPTION", 280, 50);
        g.setFont(option);
        g.setColor(new Color(50, 50, 50));
        g.fillRect(130, 450, 300, 50);
        g.fillRect(130, 150, 520, 50);
        g.setColor(Color.white);
        g.drawString("TIME", 150, 185);
        g.drawString("OK!!", 150, 485);
        g.drawImage(fist, 50, 150+100*cursor, 70, 50, this);
        g.drawImage(change, 300, 160, 30, 30, this);
        g.drawImage(change, 465, 160, -30, 30, this);
        g.setColor(new Color(200, 200, 0));
        g.drawString(Integer.toString(time), 360, 185);
        g.drawString("SEC", 500, 185);
    }

    public Font loadFont(String filename, float size){
        try {
            InputStream is = getClass().getResourceAsStream(filename);
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            font = font.deriveFont(size);
            return font;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }catch (FontFormatException ffe){
            ffe.printStackTrace();
        }
        return font;
    }

    public void keyPressed(KeyEvent e){
        int key = e.getKeyCode();
        switch(key){
            case KeyEvent.VK_ENTER:
                if(cursor == 3){
                    uecFighter.setTime(time);
                    uecFighter.callScene(0);
                }
                break;
            case KeyEvent.VK_UP:
                if(cursor > 0){
                    cursor -= 3;
                }
                break;
            case KeyEvent.VK_DOWN:
                if(cursor < 3){
                    cursor += 3;
                }
                break;
            case KeyEvent.VK_LEFT:
                if(cursor == 0){
                    if(time > 60){
                        time -= 60;
                    }
                }
                break;
            case KeyEvent.VK_RIGHT:
                if(cursor == 0){
                    if(time < 300){
                        time += 60;
                    }
                }
                break;
        }
        this.repaint();
    }

    public void keyReleased(KeyEvent e){

    }
}

public class UECFighter extends JFrame implements KeyListener{
    private UECFrameView uecFrameView;
    private StartFrameView startFrameView;
    private PlayerSelect playerselect;
    private Option option;
    private int P1, P2, time = 120;
    private static int scene = 0;
    private boolean optioned;
    public final static int SCREEN_WIDTH = 720, SCREEN_HEIGHT = 600;

    public UECFighter(){
        this.addKeyListener(this);
        this.setFocusable(true);
        this.callScene(0);
        this.setSize(SCREEN_WIDTH,  SCREEN_HEIGHT);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.setVisible(true);
    }

    public void callScene(int scene){
        this.setScene(scene);
        switch(scene){
            case 0: //スタート画面
                if(optioned){
                    option.setVisible(false);
                    this.remove(option);
                    optioned = false;
                }
                startFrameView = new StartFrameView(this);
                this.add(startFrameView);
                break;
            case 1: //キャラ選択
                startFrameView.setVisible(false);
                this.remove(startFrameView); startFrameView = null;//ガベージコレクションの対象
                playerselect = new PlayerSelect(this);
                this.add(playerselect);
                break;
            case 2: //ゲーム画面
                playerselect.setVisible(false);
                this.remove(playerselect); playerselect = null;//ガベージコレクションの対象
                uecFrameView = new UECFrameView(this, P1, P2, time);
                this.add(uecFrameView);
                break;
            case 3: //オプション画面
                startFrameView.setVisible(false);
                this.remove(startFrameView);
                optioned = true;
                option = new Option(this);
                this.add(option);
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e){

    }

    @Override
    public void keyPressed(KeyEvent e){
        if(scene == 0) startFrameView.keyPressed(e);
        else if(scene == 1) playerselect.keyPressed(e);
        else if(scene == 2) uecFrameView.keyPressed(e);
        else if(scene == 3) option.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e){
        if(scene == 0) startFrameView.keyReleased(e);
        else if(scene == 1) playerselect.keyPressed(e);
        else if(scene == 2) uecFrameView.keyReleased(e);
        else if(scene == 3) option.keyReleased(e);
    }

    public void setScene(int scene){ this.scene = scene; }

    public void setPlayer(int P1, int P2){
        this.P1 = P1; this.P2 = P2;
    }

    public void setTime(int time) { this.time = time; }

    //UECFrameViewでどのplayerを描画するかを決めるための関数

    public static void main(String argv[]){
        new UECFighter();
    }
}
