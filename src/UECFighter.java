import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.applet.AudioClip;

import java.util.*;

//操作キャラベース-----------------------------------------------------------

abstract class UECPlayerBase  {
    private final int K_UP, K_DOWN, K_LEFT, K_RIGHT, K_WeakAttack, K_StrongAttack;//操作ボタンコード
    protected boolean bK_UP = false, bK_DOWN = false, bK_LEFT = false, bK_RIGHT = false, bK_WeakAttack = false, bK_StrongAttack = false;//操作ボタンのフラグ
    protected boolean is_Jump = false, is_HighJump = false, is_Dash = false, is_Squat = false;//ジャンプ,二段ジャンプ中かダッシュ中かしゃがみ中かどうか
    protected int walkCount;//歩いている時に足踏みを管理する

    protected boolean canAttack = true;//攻撃行動できるかどうか
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
    protected boolean RequestedPlayAudio = false;//音楽再生のリクエスト。流して欲しいならtrue;
    protected String NowRequestedPlayAudio;//リクエストする名前

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

        if(canAttack && stun <= 0){
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
                    if()
                    NowImageName = "Stand";
                    canDash--;
                    if (is_Dash) {
                        is_Dash = false;
                    }
                    if (!is_Jump) {
                        dx = 0;
                    }
                }

                if (bK_UP) {
                    Jump(false);
                } else if (bK_DOWN) {
                    Squat(false);
                }
            }

        }else{
            //デバッグ
            if(Debugmessage){System.out.println(stun);}
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
    public boolean ConfirmDamage(Point2D.Float Force, int Stun, int damage){
        if(canBlock){
            //デバッグ
            if(Debugmessage){System.out.println("Blocked");}

            //ブロック出来る時
            HP -= damage/5;
            stun = 8;
            dx = 0;
        }else{
            //デバッグ
            if (Debugmessage){System.out.println("Directed");}

            //出来ない時
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



    //デバッグ
    public void setDebugmessage(boolean debugmessage){Debugmessage = debugmessage;}

}

//操作キャラA
class FighterA extends UECPlayerBase{

    public FighterA(int K_UP, int K_DOWN, int K_LEFT, int K_RIGHT, int K_WeakAttack, int K_StrongAttack, float positionX, float positionY, float magnification, Point size, Point range, Point startRange, boolean is_Right){
        super(K_UP, K_DOWN, K_LEFT, K_RIGHT, K_WeakAttack, K_StrongAttack, positionX, magnification, size, range, startRange, is_Right);
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
        else if((is_Squat = !is_Jump) && dx != 0.0f){dx *= 0.3f;}

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
                            RequestedPlayAudio = true;
                            NowRequestedPlayAudio = "Punch";
                            attackInfo.setInfo(new Point(100, 30), new Point(40, 40), 0, 5, 0, 9999, 10, new Point2D.Float(3f, -5f), 7, 1);
                            attackId = 1;
                            canCombo = 7;
                            canAttack = false;
                            break;
                        case 1:
                            NowImageName = "Punch2";
                            RequestedPlayAudio = true;
                            attackInfo.setInfo(new Point(100, 30), new Point(50, 40), 0, 5, 0, 9999, 10, new Point2D.Float(3f, -10f), 7, 2);
                            attackId = 2;
                            canCombo = 7;
                            canAttack = false;
                            break;
                        case 2:
                            NowImageName = "Kick3";
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
            System.out.println(walkCount);
            NowImageName = (walkCount = ((walkCount + 1) % 20)) / 10 == 0 ? "Stand" : "Walk";

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
    private float magnification;
    private Point OwnSize, OpponentSize, OpponentRange, OpponentStartRange;//互いのプレイヤーの大きさ, 相手の当たり判定のサイズ, 相手の当たり判定の開始位置(右基準)
    private boolean isHaving = false;//攻撃情報を所持している
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

    public void setInfo(Point StartingPoint, Point RangePoint, int Occurrence,  int Duration, int Interval,int ContinuousHit, int Damage, Point2D.Float Force, int stun, int id){
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
            rightOwnPos = new Point2D.Float(UECFighter.SCREEN_WIDTH - OwnPos.x - OwnSize.x, OwnPos.y);
            rightOpponentPos = new Point2D.Float(UECFighter.SCREEN_WIDTH - OpponentPos.x - OpponentSize.x, OpponentPos.y);
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

//スタート画面
class StartFrameView extends JPanel{// implements KeyListener {
    private JLabel character, start, option, cursole;
    private UECFighter uecFighter;
    private String[] argv = new String[4];
    private int c_point;

    public StartFrameView(UECFighter uecFighter){
        this.uecFighter = uecFighter;
        this.setSize(UECFighter.SCREEN_WIDTH, UECFighter.HEIGHT);
        this.setBackground(Color.black);
        this.setLayout(null);
        c_point = 200;

        cursole = new JLabel("->", JLabel.CENTER);
        cursole.setForeground(Color.green);
        cursole.setBounds(280, c_point, 20, 20);
        /*character = new JLabel("キャラ選択", JLabel.CENTER);
        character.setBounds(320, 200, 80, 20);
        character.setForeground(Color.white);*/
        option = new JLabel("OPTION", JLabel.CENTER);
        option.setBounds(320, 250, 80, 20);
        option.setForeground(Color.white);
        start = new JLabel("START", JLabel.CENTER);
        start.setBounds(320, 200, 80, 20);
        start.setForeground(Color.white);
        this.add(cursole);
        this.add(option);
        this.add(start);

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
                if(c_point > 200) c_point -= 50;
                this.repaint();
                break;
            case KeyEvent.VK_DOWN:
                if(c_point < 250) c_point += 50;
                this.repaint();
                break;
            case KeyEvent.VK_ENTER:
                if(c_point == 200)
                    uecFighter.callScene(1);
                else
                    uecFighter.callScene(3);
                break;
        }
    }

    //@Override
    public void keyReleased(KeyEvent e){

    }

    public void paint(Graphics g){
        super.paint(g);
        cursole.setBounds(250, c_point, 20, 20);
    }
}

//キャラ選択画面
class PlayerSelect extends JPanel {

    /*キャラ選択実装で必要なもの...
        選択されているかどうかを判定、選択をキャンセル、試合に進めるかどうかを判断するもの*/

    private UECFighter uecFighter;
    private JLabel player1, player2, p_1, p_2, OK_1, OK_2;    //player
    //private JButton p_1, p_2;
    private int p_enabled[], p_x[], p_y[], p_num,
                p1_right, p1_left, p1_up, p1_down, p1_OK, p1_cancel,
                p2_right, p2_left, p2_up, p2_down, p2_OK, p2_cancel;
    private boolean p_selected[];
    private ImageIcon image_1, image_2;
    private Image img[];
    private Point p1, p2;

    public PlayerSelect(UECFighter uecFighter){
        this.uecFighter = uecFighter;
        p_num = 2;
        p_enabled = new int[p_num];
        p_x = new int[p_num];
        p_y = new int[p_num];
        img = new Image[2];
        p_selected = new boolean[p_num];
        for(int i=0; i<p_num; i++){
            p_selected[i] = false;
            p_enabled[i] = 0;
            p_x[i] = 50 + 60*i;
            p_y[i] = 50;
        }
        p1_right = KeyEvent.VK_D; p1_left = KeyEvent.VK_A; //p1_up = KeyEvent.VK_W; p1_down = KeyEvent.VK_S;
        p1_OK = KeyEvent.VK_C; p1_cancel = KeyEvent.VK_V;
        p2_right = KeyEvent.VK_L; p2_left = KeyEvent.VK_J; //p2_up = KeyEvent.VK_I; p2_down = KeyEvent.VK_K;
        p2_OK = KeyEvent.VK_COMMA; p2_cancel = KeyEvent.VK_PERIOD;
        this.setSize(uecFighter.SCREEN_WIDTH, uecFighter.SCREEN_HEIGHT);
        this.setBackground(Color.black);
        this.setLayout(null);
        image_1 = new ImageIcon("resources/kiuch.JPG");
        image_2 = new ImageIcon("resources/takayuki.jpg");
        img[0] = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/kiuch.JPG"));
        img[1] = Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/takayuki.jpg"));
        p1 = new Point(10, 10);
        p2 = new Point(10, 60);
        p_1 = new JLabel(image_1);
        p_2 = new JLabel(image_2);
        p_1.setBounds(p_x[0], p_y[0], 50, 50);
        p_2.setBounds(p_x[1], p_y[1], 50, 50);
        p_1.setOpaque(true);
        p_2.setOpaque(true);
        this.add(p_1);
        this.add(p_2);
    }

    public void keyTyped(KeyEvent e){

    }

    public void keyPressed(KeyEvent e){
        int key = e.getKeyCode();
        if(key == p1_right){
            p_enabled[0] += 1;
        }else if(key == p1_left){
            p_enabled[0] -= 1;
        }else if(key == p2_right){
            p_enabled[1] += 1;
        }else if(key == p2_left){
            p_enabled[1] -= 1;
        }else if(key == p1_OK){
            p_selected[0] = true;
        }else if(key == p2_OK){
            p_selected[1] = true;
        }else if(key == p1_cancel){
            p_selected[0] = false;
        }else if(key == p2_cancel){
            p_selected[1] = false;
        }else if(key == KeyEvent.VK_ENTER){
            if(p_selected[0] && p_selected[1]){
                uecFighter.callScene(2);
            }
        }
        repaint();
    }

    public void keyReleased(KeyEvent e){

    }

    @Override
    public void paint(Graphics g){
        super.paint(g);

        for(int i=0; i<p_num; i++){
            g.setColor(Color.red);
            g.drawRect(50+(p_enabled[0])*60-2, p_y[0]-2, 52, 52);
            g.fillOval(50+(p_enabled[0])*60-12, p_y[0]-2, 10, 10);
            g.fillRect(190, 290, 120, 120);
            g.setColor(Color.blue);
            g.drawRect(50+(p_enabled[1])*60-2, p_y[1]-2, 52, 52);
            g.fillOval(50+(p_enabled[1])*60-12, p_y[1]+8, 10, 10);
            g.fillRect(440, 290, 120, 120);
        }
        if(p_selected[0] == true){
            g.drawImage(img[p_enabled[0]], 200, 300, 100, 100, this);
        }
        if(p_selected[1] == true){
            g.drawImage(img[p_enabled[1]], 450, 300, 100, 100, this);
        }
    }
}

//試合時間を管理
class GameTime implements ActionListener{
    private javax.swing.Timer gameTime;
    private int time;

    public GameTime(int time){
        this.time = time;
        gameTime = new javax.swing.Timer(1000, this);
        gameTime.start();
    }

    public String getTime(){
        return String.format("%d", time);
    }

    public void setTime(){
        time--;
    }

    public void actionPerformed(ActionEvent e){
        this.setTime();
    }
}

//試合画面
class UECFrameView extends JPanel {//implements KeyListener{
    private java.util.Timer gameThread;//ゲーム用スレッド
    private UECFighter uecFighter;
    private GameTime gameTime;
    private JLabel timeLabel;
    private int scene; //ゲームのシーンをmanegementする為の変数

    private UECPlayerBase player1, player2;
    private Point2D.Float p1position, p2position;
    private Point p1size, p2size, p1range, p2range, p1startRange, p2startRange;
    private Image p1image, p2image;
    private AttackInfo p1AttackInfo, p2AttackInfo;

    private TreeMap<String, AudioClip> audios;

    public UECFrameView(UECFighter uecFighter){
        this.uecFighter = uecFighter;
        this.setSize(UECFighter.SCREEN_WIDTH, UECFighter.HEIGHT);
        this.setBackground(new Color(150,255,255));
        this.setLayout(null);

        //プレイヤー1
        float p1magnification = 2f;
        p1size = new Point((int) (120 * p1magnification), (int) (120 * p1magnification));
        p1range = new Point((int) (30 * p1magnification), (int) (70 * p1magnification));
        p1startRange = new Point((int) (45 * p1magnification), (int) (50 * p1magnification));
        player1 = new FighterA(KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,  KeyEvent.VK_C, KeyEvent.VK_V, 0, UECFighter.SCREEN_HEIGHT-p1size.y, p1magnification, p1size, p1range, p1startRange, true);

        //プレイヤー2
        float p2magnification = 2f;
        p2size = new Point((int) (120 * p2magnification), (int) (120 * p2magnification));
        p2range = new Point((int) (30 * p2magnification), (int) (70 * p2magnification));
        p2startRange = new Point((int) (45 * p2magnification), (int) (50 * p2magnification));
        player2 = new FighterA(KeyEvent.VK_I, KeyEvent.VK_K, KeyEvent.VK_J, KeyEvent.VK_L,  KeyEvent.VK_COMMA, KeyEvent.VK_PERIOD, UECFighter.SCREEN_WIDTH-p2size.x, UECFighter.SCREEN_HEIGHT-p2size.y, p2magnification, p2size, p2range, p2startRange, false);

        //互いの対戦相手の情報を交換する
        player1.setOpponentPlayer(player2);
        player2.setOpponentPlayer(player1);
        //攻撃情報管理クラス(AttackInfo)を準備する
        player1.setAttackInfo(player2);
        player2.setAttackInfo(player1);

        //効果音登録
        audios = new TreeMap<String, AudioClip>();
        RegisterAudioClip();

        gameTime = new GameTime(120);
        Font font = new Font(Font.SANS_SERIF, JLabel.CENTER, 32);
        timeLabel = new JLabel("120", JLabel.CENTER);
        timeLabel.setBackground(Color.WHITE);
        timeLabel.setBounds(320, 0, 80, 80);
        timeLabel.setFont(font);
        timeLabel.setOpaque(true);
        this.add(timeLabel);

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

        timeLabel.setText(gameTime.getTime());

        //当たり判定デバッグ用
        p1AttackInfo.print(g, p1position);
        p2AttackInfo.print(g, p2position);
        //
    }

    //@Override
    public void keyTyped(KeyEvent e) {
        requestFocusInWindow();
    }

    //@Override
    public void keyPressed(KeyEvent e) {
        player1.keyPressed(e);
        player2.keyPressed(e);
    }

    //@Override
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

//オプしション画面
class Option extends JPanel {
    private UECFighter uecFighter;
}

public class UECFighter extends JFrame implements KeyListener{
    private UECFrameView uecFrameView;
    private StartFrameView startFrameView;
    private PlayerSelect playerselect;
    private static int scene = 0;
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
                startFrameView = new StartFrameView(this);
                this.add(startFrameView);
                break;
            case 1: //キャラ選択
                startFrameView.setVisible(false);
                this.remove(startFrameView);
                playerselect = new PlayerSelect(this);
                this.add(playerselect);
                break;
            case 2: //ゲーム画面
                playerselect.setVisible(false);
                this.remove(playerselect);
                uecFrameView = new UECFrameView(this);
                this.add(uecFrameView);
                break;
            case 3: //オプション画面
                startFrameView.setVisible(false);
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e){

    }

    @Override
    public void keyPressed(KeyEvent e){
        if(scene == 0) startFrameView.keyPressed(e);
        else if(scene == 2) uecFrameView.keyPressed(e);
        else if(scene == 1) playerselect.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e){
        if(scene == 0) startFrameView.keyReleased(e);
        else if(scene == 2) uecFrameView.keyReleased(e);
    }

    public void setScene(int scene){
        this.scene = scene;
    }

    public static void main(String argv[]){
        new UECFighter();
    }
}
