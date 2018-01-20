# OriginGameForMedia

## 全体

* ここに各自の変更点を書き込んでいこう(書き方は"Markdown"で検索、むずくないよ)
* srcフォルダ下にソースコードまとめてます。画像・音データはsrc/resourceに
* 完成品はclassファイルであれば良いのでjavaファイルはそれぞれ固有ので持っててくれれば良いのかな？初回なので全部pushしたけど、固有javaファイルはともかく **クラスファイルのpushは雑にやってはいけないでしょう。どのクラスファイルをpush(新規or上書き)するか吟味してからね…**
* uecワイヤレスではpush出来ないみたいなので更新は家でやることになりそう。現在調査中。
* ChinChinFighter.classを基盤のクラスとしている。12/15現在はChinChinFrameViewを呼び出すだけだが、可能ならばモード選択とかキャラ選択画面の呼び出しとかしたいね。

## なおちゃん

* 12/15
  - 抽象クラスChinChinPlayerBaseを作成し、それを基盤にクラスFighterA(ダミーキャラ)を作った。 
  - ChinChinFrameViewを作成してキャラを表示させる所もついでに作った。(てかこれないとデバッグ出来ないねん。ゆるちて)ここからはちんぽっぽに委ねたいと思う。
  - 基本の移動制御は出来てきた。来週に向けてはキャラの反転制御・攻撃判定およびノックバック制御頑張ります。
* 12/21
  - 操作キャラの向いている方向を表すクラスLookingを作った。(左=-1;右=1;) #詳しくは「列挙型」で検索
  - 攻撃情報を管理するためのクラスAttackInfoを作った。このクラスでは当たり範囲や継続時間などを保持し、判定を行う予定。プレイヤーにArrayList型で実装するつもり。
* 12/29
  - 攻撃の判定を管理するクラスAttackInfoの大まかな仕組みが仕上がったよ。と、同時にコンボを成立させる仕組みまで作りました。解読よろしくね。(解読はFighterAクラスのWeakAttackメソッドを見れば大体理解できるはず)
  - きうちの作ったHPバーをマージしました。その際HPのアクセサメソッドは抽象メソッドではなくPlayerBaseクラスのメソッドとして追加しました。
  - gameThreadのライフサイクルは現状、"p1p2のAction → p1p2のAttackInfo → p1p2と攻撃の確定(ConfirmAttack) → paint"の順です。
* 1/9
  - CCPlayerBaseとAttackInfoを用いた攻撃情報の管理機構の基盤を完成させました。これからも完成させるべき機能はまだ残っているけど、とりあえず形になった感じ。攻撃の設定やコンボの機能なんかも一通り作ったのでもう技の設定は出来るので、**わたるには現状のキャラ設定を搭載してみて欲しいです。**
  - きうちの作ったコードは現状では統合してないです

## わたる

* 12/22
  - SoundTest.javaあげました。
  
* 音声ファイルリクエスト
  - ガード音
  - 着地音 OK
  - 攻撃音各位 OK
  - 選択画面(移動、決定) OK

## ちんぽっぽ

* 12/22
  - キャラクターの位置によって向きが変わるようにしました。
* 12/28
  - HPの表示を作りました。class ChinChinPlayerBaseに、メソッドgetHPとsetHPを追加しました。
