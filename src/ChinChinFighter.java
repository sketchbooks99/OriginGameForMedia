import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.applet.AudioClip;

import java.util.*;

//操作キャラベース-----------------------------------------------------------

abstract class ChinChinPlayerBase extends JPanel {
    private final int K_UP, K_DOWN, K_LEFT, K_RIGHT, K_WeakAttack, K_StrongAttack;//操作ボタンコード
    protected boolean bK_UP = false, bK_DOWN = false, bK_LEFT = false, bK_RIGHT = false, bK_WeakAttack = false, bK_StrongAttack = false;//操作ボタンのフラグ
    protected boolean is_Jump = false, is_HighJump = false, is_Dash = false, is_Squat = false;//ジャンプ,二段ジャンプ中かダッシュ中かしゃがみ中かどうか

    protected boolean canAttack = true;//攻撃行動できるかどうか
    protected int canCombo = 0, attackId = -1;//コンボ技の入力受付時間を管理, 前回攻撃idを引き継ぐ(-1はダミーコード)
    protected AttackInfo attackInfo;//攻撃情報を収納する

    protected boolean before_Right = false;//直前の左右入力を保存する
    protected boolean canBlock = false;//ガード受付状態の管理
    protected int HP;//キャラの体力
    protected Point2D.Float position; //キャラの座標
    protected float dy, dx;
    protected Point size, range, startRange;//キャラのサイズ, あたり範囲のサイズ, あたり判定のスタート(右向き基準, 左向き基準)
    protected int underLine, rightLine;//ステージ下限右限
    protected int stun, canDash;//行動不自由時間, ダッシュ状態を受け付けている時間
    protected Looking look; //右を向いているか
    protected ChinChinPlayerBase OpponentPlayer;//対戦相手

    protected TreeMap<String, Image> Images;//グラフィック管理
    protected String NowImageName;//呼び出す画像の名前
    protected boolean RequestedPlayAudio = false;//音楽再生のリクエスト。流して欲しいならtrue;
    protected String NowRequestedPlayAudio;//リクエストする名前

    protected boolean Debugmessage = false;

    public ChinChinPlayerBase(int K_UP, int K_DOWN, int K_LEFT, int K_RIGHT, int K_WeakAttack, int K_StrongAttack, float positionX, Point size, Point range, Point startRange, boolean is_Right){
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

        underLine = ChinChinFighter.SCREEN_HEIGHT * 9 / 10 - size.y;
        rightLine = ChinChinFighter.SCREEN_WIDTH - size.x;

        //座標設定
        position = new Point2D.Float(positionX, underLine);
        this.size = size;
        this.range = range;
        this.startRange = startRange;

        //画像設定
        Images = new TreeMap<String, Image>();
        RegisterImage();
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
    abstract protected void Jump(boolean Pressed);
    //しゃがみモーション
    abstract protected void Squat(boolean Pressed);
    //弱攻撃
    abstract protected void WeakAttack(boolean Pressed);
    //強攻撃
    abstract protected void StrongAttack(boolean Pressed);
    //移動
    abstract protected void Move(boolean is_Right, boolean Pressed);
    //一連の行動を管理する
    public void Action(){
        //デバッグ
        if(Debugmessage){System.out.println(canAttack + " " + stun);}

        if(canAttack && stun <= 0){
            //デバッグ
            if(Debugmessage){System.out.println("B");}
            if(!attackInfo.isHaving()) {

                if (attackId != -1 && canCombo-- <= 0) {
                    attackId = -1;
                }
                if (bK_WeakAttack) {
                    WeakAttack(false);
                } else if (bK_StrongAttack) {
                    StrongAttack(false);
                }

                if (bK_LEFT || bK_RIGHT) {
                    Move(bK_RIGHT, false);
                } else {
                    canDash--;
                    if (is_Dash) {
                        is_Dash = false;
                    }
                    if (!is_Jump) {
                        dx = 0;
                        if(Debugmessage){System.out.println("A");}
                    }
                }

                if (bK_UP) {
                    Jump(false);
                } else if (bK_DOWN) {
                    Squat(false);
                }
            }

        }else{
            stun--;
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
    public String getNowRequestedPlayAudio(){
        if(RequestedPlayAudio){
            RequestedPlayAudio = false;
            return NowRequestedPlayAudio;
        }
        return null;
    }

    public Point2D.Float getPosition(){ return position; }

    //ダメージ情報を扱う
    public boolean ConfirmDamage(Point2D.Float Force, int stun, int damage){
        if(canBlock){
            //デバッグ
            if(Debugmessage){System.out.println("Blocked");}

            //ブロック出来る時
            HP -= damage/2;
        }else{
            //デバッグ
            if (Debugmessage){System.out.println("Directed");}

            //出来ない時
            HP -= damage;
            this.stun = stun;
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

    public void setOpponentPlayer(ChinChinPlayerBase opponentPlayer) { OpponentPlayer = opponentPlayer; }

    public AttackInfo getAttackInfo(){ return attackInfo; }
    public void setAttackInfo(ChinChinPlayerBase Opponent){ attackInfo = new AttackInfo(this, Opponent); }
    public void setCanAttack(boolean canAttack){ this.canAttack = canAttack; }

    public void setDebugmessage(boolean debugmessage){Debugmessage = debugmessage;}

}

//操作キャラA
class FighterA extends ChinChinPlayerBase{

    public FighterA(int K_UP, int K_DOWN, int K_LEFT, int K_RIGHT, int K_WeakAttack, int K_StrongAttack, float positionX, float positionY, Point size, Point range, Point startRange, boolean is_Right){
        super(K_UP, K_DOWN, K_LEFT, K_RIGHT, K_WeakAttack, K_StrongAttack, positionX, size, range, startRange, is_Right);
        HP = 100;//体力セット
    }

    @Override
    protected void Jump(boolean Pressed) {
        if(Pressed && (!is_Jump || !is_HighJump)){
            if(!is_Jump){
                is_Jump = true;
            }else{
                is_HighJump = true;
            }
            dy = -30.0f;
        }
    }

    @Override
    protected void Squat(boolean Pressed) {
        if (Pressed){
            if(is_Jump){dx *= 0.5f;}
            is_Dash = false;
        }
        else if(!is_Jump && dx != 0.0f){dx *= 0.3f;}
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
                            NowImageName = "Punch";
                            RequestedPlayAudio = true;
                            NowRequestedPlayAudio = "Punch";
                            attackInfo.setInfo(new Point(100, 30), new Point(40, 40), 0, 5, 0, 9999, 10, new Point2D.Float(3f, -5f), 7, 1);
                            attackId = 1;
                            canCombo = 7;
                            canAttack = false;
                            break;
                        case 1:
                            NowImageName = "Punch";
                            RequestedPlayAudio = true;
                            attackInfo.setInfo(new Point(100, 30), new Point(50, 40), 0, 5, 0, 9999, 10, new Point2D.Float(3f, -10f), 7, 2);
                            attackId = 2;
                            canCombo = 7;
                            canAttack = false;
                            break;
                        case 2:
                            NowImageName = "Punch";
                            RequestedPlayAudio = true;
                            attackInfo.setInfo(new Point(100, 30), new Point(70, 40), 0, 20, 10, 9999, 10, new Point2D.Float(30f, -20f), 30, -1);
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
                switch (attackId) {
                    case -1:
                        attackInfo.setInfo(new Point(10, 10), new Point(40, 40), 15, 20, 10, 9999,10, new Point2D.Float(40f,10f), 20, 11);
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
            if(goAhead && Pressed && !is_Jump && !bK_DOWN){
                if(canDash > 0){is_Dash = true;}
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
        Images.put("Stand", toolkit.getImage(getClass().getResource("resources/playerADammy.png")));
        Images.put("Punch", toolkit.getImage(getClass().getResource("resources/playerADammyPunch.png")));
    }

    @Override
    public void RegisterAudioClip(TreeMap<String, AudioClip> audios) {

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
    private Point OwnSize, OpponentSize, OpponentRange, OpponentStartRange;//互いのプレイヤーの大きさ, 相手の当たり判定のサイズ, 相手の当たり判定の開始位置(右基準)
    private boolean isHaving = false;//攻撃情報を所持している
    private int ContinuousHit, NowContinuousHit;//多段ヒット攻撃の時、次弾ヒットまでの時間を登録。単発攻撃なら9999をセットする。
    private Point2D.Float Force;//攻撃時の加力ベクトル
    private Point2D.Float OwnPos;//自身の座標
    private ChinChinPlayerBase Own, Opponent;
    private Looking look;//右向いているか否か



    public AttackInfo(ChinChinPlayerBase Own, ChinChinPlayerBase Opponent){
        this.OwnSize = Own.getMySize();
        this.Own = Own;
        this.Opponent = Opponent;
        this.OpponentSize = Opponent.getMySize();
        this.OpponentRange = Opponent.getRange();
        this.OpponentStartRange = Opponent.getStartRange();
    }

    public void setInfo(Point StartingPoint, Point RangePoint, int Occurrence,  int Duration, int Interval,int ContinuousHit, int Damage, Point2D.Float Force, int stun, int id){
        this.StartingPoint = StartingPoint;
        this.RangePoint = RangePoint;
        this.sOccurrence = Occurrence;
        this.sDuration = Duration + Occurrence;
        this.sInterval = Interval + Duration + Occurrence;
        this.ContinuousHit = this.NowContinuousHit = ContinuousHit;
        this.Damage = Damage;
        this.Force = Force;
        this.stun = stun;
        this.id = id;
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

        //自身の向きを引数に。向きが左ならば全てを反転して右向きとして考える。
        this.look = look;
        this.OwnPos = OwnPos;
        Point2D.Float rightOwnPos, rightOpponentPos;
        if(look == Looking.Right){
            rightOwnPos = OwnPos;
            rightOpponentPos = OpponentPos;
        }else{
            rightOwnPos = new Point2D.Float(ChinChinFighter.SCREEN_WIDTH - OwnPos.x - OwnSize.x, OwnPos.y);
            rightOpponentPos = new Point2D.Float(ChinChinFighter.SCREEN_WIDTH - OpponentPos.x - OpponentSize.x, OpponentPos.y);
        }

        boolean isHitted = rightOwnPos.x + StartingPoint.x + RangePoint.x >= rightOpponentPos.x && rightOwnPos.x + StartingPoint.x <= rightOpponentPos.x + OpponentSize.y &&
                rightOwnPos.y + StartingPoint.y + RangePoint.y >= rightOpponentPos.y && rightOwnPos.y + StartingPoint.y <= rightOpponentPos.y + OpponentSize.y;
        //デバッグ
        if(isHitted){
            System.out.println("Hit");
        }
        alreadyHit = isHitted;
        return isHitted;
    }

    public void ConfirmAttack(){
        System.out.println(Force != null);
        Opponent.ConfirmDamage(new Point2D.Float(Force.x * look.getValue(), Force.y), stun, Damage);
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

class ChinChinFrameView extends JPanel implements KeyListener{
    private java.util.Timer gameThread;//ゲーム用スレッド
    private ChinChinFighter chinChinFighter;

    private ChinChinPlayerBase player1, player2;
    private Point2D.Float p1position, p2position;
    private Point p1size, p2size, p1range, p2range, p1startRange, p2startRange;
    private Image p1image, p2image;
    private AttackInfo p1AttackInfo, p2AttackInfo;

    private TreeMap<String, AudioClip> audios;

    int c = 0;//デバッグ

    public ChinChinFrameView(ChinChinFighter chinChinFighter){
        this.chinChinFighter = chinChinFighter;
        this.setSize(ChinChinFighter.SCREEN_WIDTH, ChinChinFighter.HEIGHT);
        this.setBackground(new Color(150,255,255));
        this.setLayout(new GridLayout(1,2));

        //プレイヤー1
        p1size = new Point(120, 120);
        p1range = new Point(30, 70);
        p1startRange = new Point(45, 50);
        player1 = new FighterA(KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,  KeyEvent.VK_C, KeyEvent.VK_V, 0, ChinChinFighter.SCREEN_HEIGHT-p1size.y, p1size, p1range, p1startRange, true);

        //プレイヤー2
        p2size = new Point(120 , 120);
        p2range = new Point(30, 70);
        p2startRange = new Point(45, 50);
        player2 = new FighterA(KeyEvent.VK_I, KeyEvent.VK_K, KeyEvent.VK_J, KeyEvent.VK_L,  KeyEvent.VK_COMMA, KeyEvent.VK_PERIOD, ChinChinFighter.SCREEN_WIDTH-p2size.x, ChinChinFighter.SCREEN_HEIGHT-p2size.y, p2size, p2range, p2startRange, false);

        //互いの対戦相手の情報を交換する
        player1.setOpponentPlayer(player2);
        player2.setOpponentPlayer(player1);
        //攻撃情報管理クラス(AttackInfo)を準備する
        player1.setAttackInfo(player2);
        player2.setAttackInfo(player1);

        //効果音登録
        audios = new TreeMap<String, AudioClip>();
        RegisterAudioClip();

        setFocusable(true);
        addKeyListener(this);

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

                //攻撃を扱う
                p1AttackInfo = player1.getAttackInfo();
                p2AttackInfo = player2.getAttackInfo();
                boolean p2Hitted = false, p1Hitted = false;
                int p1mode, p2mode;
                //"AttackInfo.update()==1" means "Attacking".
                if(p1AttackInfo.isHaving()){
                    switch (p1mode = p1AttackInfo.update()){
                        case 1:
                            p2Hitted = p1AttackInfo.judgeHitted(player1.getLook(), p1position, p2position);
                            break;
                        case 3:
                            System.out.println(p1mode + "called " + c++);
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
    public void paint(Graphics g){
        super.paint(g);
        //位置を渡してついでにJump判定もやってもらうことにした
        if((p1image = player1.getNowImage()) != null) {
            //(p1 < p2) -> lookingRight
            if(player1.setgetLook(p2position) == Looking.Right){
                g.drawImage(p1image,
                        (int) p1position.x, (int) p1position.y,
                        p1size.x, p1size.y, null);
            }else{
                g.drawImage(p1image,
                        (int) p1position.x + p1size.x, (int) p1position.y,
                        -p1size.x, p1size.y, null);
            }
        }
        if((p2image = player2.getNowImage()) != null) {
            //p2 < p1 -> lookingRight
            if(player2.setgetLook(p1position) == Looking.Right){
                g.drawImage(p2image,
                        (int) p2position.x, (int) p2position.y,
                        p2size.x, p2size.y, null);
            }else{
                g.drawImage(p2image,
                        (int) p2position.x + p2size.x, (int) p2position.y,
                        -p2size.x, p2size.y, null);
            }
        }

        //fillRectでHPを表示してます。
        g.setColor(new Color(0, 200, 0));
        g.fillRect(320-player1.getHP()*3, 10, player1.getHP()*3, 20);
        g.fillRect(400, 10, player2.getHP()*3, 20);

        //SE再生
        String p1RequestName = player1.getNowRequestedPlayAudio();
        if (p1RequestName!= null){ PlaySoundEffect(p1RequestName);}
        String p2RequestName = player2.getNowRequestedPlayAudio();
        if (p2RequestName!= null){ PlaySoundEffect(p2RequestName);}

        //当たり判定デバッグ用
        p1AttackInfo.print(g, p1position);
        p2AttackInfo.print(g, p2position);
        //
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        player1.keyPressed(e);
        player2.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        player1.keyReleased(e);
        player2.keyReleased(e);
    }

    private void RegisterAudioClip(){
        audios.put("Punch", java.applet.Applet.newAudioClip(getClass().getResource("resources/punch_middle.wav")));
        player1.RegisterAudioClip(audios);
        player2.RegisterAudioClip(audios);
    }

    private void PlaySoundEffect(String reqestedSoundName){
        AudioClip audioClip = audios.get(reqestedSoundName);

        audioClip.play();
    }

}

public class ChinChinFighter extends JFrame {
    private ChinChinFrameView chinChinFrameView;
    public final static int SCREEN_WIDTH = 720, SCREEN_HEIGHT = 600;

    public ChinChinFighter(){
        chinChinFrameView = new ChinChinFrameView(this);
        this.setSize(SCREEN_WIDTH,  SCREEN_HEIGHT);
        this.add(chinChinFrameView);

        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.setVisible(true);
    }



    public static void main(String argv[]){
        new ChinChinFighter();
    }
}
