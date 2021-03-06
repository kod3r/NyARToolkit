package jp.qualitas.nyartoolkit.respberrypi.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Locale;
import javax.media.j3d.Node;
import javax.media.j3d.PhysicalBody;
import javax.media.j3d.PhysicalEnvironment;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.media.j3d.ViewPlatform;
import javax.media.j3d.VirtualUniverse;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import jp.nyatla.nyartoolkit.core.NyARCode;
import jp.nyatla.nyartoolkit.core.NyARException;
import jp.qualitas.nyartoolkit.java3d.utils.raspberrypi.J3dNyARParam;
import jp.qualitas.nyartoolkit.java3d.utils.raspberrypi.NyARSingleMarkerBehaviorHolder;
import jp.qualitas.nyartoolkit.java3d.utils.raspberrypi.NyARSingleMarkerBehaviorListener;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;
import com.sun.j3d.utils.geometry.ColorCube;

public class NyARJava3Dfx extends Application implements
		NyARSingleMarkerBehaviorListener {
	// set capture driver for v4l4j tool
	static {
		String osName = System.getProperty("os.name");
		String arch = System.getProperty("os.arch");
		if (osName.equals("Linux") & arch.equals("arm")) {
			// Webcam.setDriver(new FsWebcamDriver());
			// Webcam.setDriver(new GStreamerDriver());
			Webcam.setDriver(new V4l4jDriver());
		}
		// org.bridj.Platform
		// .addEmbeddedLibraryResourceRoot("com/github/sarxos/webcam/ds/buildin/lib/");
		// org.bridj.BridJ.register();
	}
	private final String CARCODE_FILE = "/data/patt.hiro";

	private final String PARAM_FILE = "/data/camera_para4.dat";

	// NyARToolkit関係
	private NyARSingleMarkerBehaviorHolder nya_behavior;

	private J3dNyARParam ar_param;

	// universe関係
	private Canvas3D canvas;

	private Locale locale;

	private VirtualUniverse universe;

	private Stage stage;

	private Group root;

	private int width = 640;
	private int height = 480;

	private Random random = new Random();

	public void onUpdate(boolean i_is_marker_exist,
			final Transform3D i_transform3d) {
		/*
		 * TODO:Please write your behavior operation code here.
		 * マーカーの姿勢を元に他の３Dオブジェクトを操作するときは、ここに処理を書きます。
		 */
		if (i_transform3d != null) {
			Vector3d vector = new Vector3d();
			i_transform3d.get(vector);

			// if (vector.lengthSquared() > 0.15) {
			System.out.println("Hit!!!");
			System.out.println("getScale()=" + i_transform3d.getScale());
			System.out.println("vector.length()=" + vector.length());
			System.out.println("vector.lengthSquared()="
					+ vector.lengthSquared());
			System.out.println("vector.getX()=" + vector.getX());
			System.out.println("vector.getY()=" + vector.getY());
			System.out.println("vector.getZ()=" + vector.getZ());
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					// appendAnimation("hiragana_01_a.png", i_transform3d);
					showHiraganaAnimation(i_transform3d);
				}
			});

			// }

			Matrix3d matrix = new Matrix3d();
			i_transform3d.get(matrix);
			System.out.println("M00" + matrix.getM00());
		}

	}

	public void startCapture() throws Exception {
		// キャプチャ開始
		nya_behavior.start();

		// localeの作成とlocateとviewの設定
		universe = new VirtualUniverse();
		locale = new Locale(universe);
		// canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
		View view = new View();
		ViewPlatform viewPlatform = new ViewPlatform();
		view.attachViewPlatform(viewPlatform);
		// view.addCanvas3D(canvas);
		view.setPhysicalBody(new PhysicalBody());
		view.setPhysicalEnvironment(new PhysicalEnvironment());

		// 視界の設定(カメラ設定から取得)
		Transform3D camera_3d = ar_param.getCameraTransform();
		view.setCompatibilityModeEnable(true);
		view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
		view.setLeftProjection(camera_3d);

		// 視点設定(0,0,0から、Y軸を180度回転してZ+方向を向くようにする。)
		TransformGroup viewGroup = new TransformGroup();
		Transform3D viewTransform = new Transform3D();
		viewTransform.rotY(Math.PI);
		viewTransform.setTranslation(new Vector3d(0.0, 0.0, 0.0));
		viewGroup.setTransform(viewTransform);
		viewGroup.addChild(viewPlatform);
		BranchGroup viewRoot = new BranchGroup();
		viewRoot.addChild(viewGroup);
		locale.addBranchGraph(viewRoot);

		// バックグラウンドの作成
		Background background = new Background();
		BoundingSphere bounds = new BoundingSphere();
		bounds.setRadius(10.0);
		background.setApplicationBounds(bounds);
		background.setImageScaleMode(Background.SCALE_FIT_ALL);
		background.setCapability(Background.ALLOW_IMAGE_WRITE);
		BranchGroup root = new BranchGroup();
		root.addChild(background);

		// TransformGroupで囲ったシーングラフの作成
		TransformGroup transform = new TransformGroup();
		transform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		transform.addChild(createSceneGraph());
		root.addChild(transform);

		// Behaviorに連動するグループをセット
		nya_behavior.setTransformGroup(transform);
		nya_behavior.setBackGround(background);

		// 出来たbehaviorをセット
		root.addChild(nya_behavior.getBehavior());
		nya_behavior.setUpdateListener(this);

		// 表示ブランチをLocateにセット
		locale.addBranchGraph(root);

		// ウインドウの設定
		// setLayout(new BorderLayout());
		// add(canvas, BorderLayout.CENTER);
	}

	@Override
	public void start(Stage stage) throws NyARException, IOException {
		this.stage = stage;
		this.showWindow();

		// //NyARToolkitの準備
		NyARCode ar_code = NyARCode.createFromARPattFile(this.getClass()
				.getResourceAsStream(CARCODE_FILE), 16, 16);
		ar_param = J3dNyARParam.loadARParamFile(this.getClass()
				.getResourceAsStream(PARAM_FILE));
		ar_param.changeScreenSize(320, 240);

		// NyARToolkitのBehaviorを作る。(マーカーサイズはメートルで指定すること)
		nya_behavior = new NyARSingleMarkerBehaviorHolder(ar_param, 30f,
				ar_code, 0.08);
		nya_behavior.setWebcapOpenListener(this);
		nya_behavior.open();
	}

	private void showWindow() throws IOException {
		root = new Group();

		Scene scene = new Scene(root, width, height);

		stage.setTitle("JavaFX ARToolkit Demo");
		stage.setScene(scene);

		stage.show();
		stage.setFullScreen(true);

		// this.appendAnimation("hiragana_01_a.png", null);
		showHiraganaAnimation(null);
	}

	// private void showHiraganaAnimation(Transform3D i_transform3d) {
	// if (currentHiraganaIndex >= hiragana_a.size()) {
	// currentHiraganaIndex = 0;
	// }
	// appendAnimation(hiragana_a.get(currentHiraganaIndex++), i_transform3d);
	// }
	private int currentHiraganaIndex = 0;

	private void showHiraganaAnimation(Transform3D i_transform3d) {
		if (currentHiraganaIndex >= katakana.size()) {
			currentHiraganaIndex = 0;
		}
		appendAnimation(katakana.get(currentHiraganaIndex++), i_transform3d);
	}

	private List<String> hiragana_a = new ArrayList<String>() {
		{
			add("hiragana_01_a.png");
		}
	};;
	private List<String> hikouki = new ArrayList<String>() {
		{
			add("hiragana_47_hi.png");
			add("hiragana_15_ko.png");
			add("hiragana_03_u.png");
			add("hiragana_12_ki.png");
		}
	};
	private List<String> katakana = new ArrayList<String>() {
		{
			add("katakana/katakana_01_a.png");
			add("katakana/katakana_02_i.png");
			add("katakana/katakana_03_u.png");
			add("katakana/katakana_05_o.png");
			add("katakana/katakana_06_a2.png");
			add("katakana/katakana_08_vu.png");
			add("katakana/katakana_10_o2.png");
			add("katakana/katakana_11_ka.png");
			add("katakana/katakana_12_ki.png");
			add("katakana/katakana_13_ku.png");
			add("katakana/katakana_15_ko.png");
			add("katakana/katakana_18_gu.png");
			add("katakana/katakana_21_sa.png");
			add("katakana/katakana_22_shi.png");
			add("katakana/katakana_24_se.png");
			add("katakana/katakana_25_so.png");
			add("katakana/katakana_28_zu.png");
			add("katakana/katakana_29_ze.png");
			add("katakana/katakana_30_zo.png");
			add("katakana/katakana_31_ta.png");
			add("katakana/katakana_32_ti.png");
			add("katakana/katakana_33_tsu.png");
			add("katakana/katakana_34_te.png");
			add("katakana/katakana_35_to.png");
			add("katakana/katakana_36_da.png");
			add("katakana/katakana_37_di.png");
		}
	};
	private boolean playing = false;

	private Rectangle currentTargetRect;
	private int rectWidth = 200;
	private int rectHeight = 150;
	private int defaultRectX = width / 2 - rectWidth / 2;
	private int defaultRectY = height / 2 - rectHeight / 2;
	private int currentRectX = width / 2 - rectWidth / 2;
	private int currentRectY = height / 2 - rectHeight / 2;

	private final int MAX_SHOW_ANIMATION_COUNT = 10;
	private int currentAnimationCount = 0;
	private long previousAnimationTime = 0;

	private boolean isShowTargetRectangle = true;

	private void showTargetRectangle(final Transform3D i_transform3d) {
		if (i_transform3d != null) {
			// かなり適当な対象物の位置特定
			Vector3d vector = new Vector3d();
			i_transform3d.get(vector);
			double addjustX = vector.getX() * 2000;
			double addjustY = vector.getY() * 2000;
			currentRectX = defaultRectX + (int) addjustX * -1;
			currentRectY = defaultRectY + (int) addjustY * -1;
		}
		if (!isShowTargetRectangle) {
			return;
		}
		if (currentTargetRect != null) {
			root.getChildren().remove(currentTargetRect);
		}

		currentTargetRect = new Rectangle(currentRectX, currentRectY,
				rectWidth, rectHeight);
		currentTargetRect.setStroke(Color.GRAY);
		currentTargetRect.setFill(null);
		root.getChildren().add(currentTargetRect);

	}

	private void appendAnimation(final String hiraganaImageName,
			final Transform3D i_transform3d) {
		showTargetRectangle(i_transform3d);

		if (currentAnimationCount > MAX_SHOW_ANIMATION_COUNT) {
			return;
		}

		long currentTime = new Date().getTime();
		long timePeriod = currentTime - previousAnimationTime;

		double period = (double) timePeriod / 1000;
		System.out.println("timePeriod=" + period);
		if (period < 0.5) {
			// アニメーション表示の間隔をあける
			return;
		}
		System.out.println("show animation.");
		previousAnimationTime = currentTime;

		currentAnimationCount++;
		int centerX = currentRectX - rectWidth / 2;
		int centerY = currentRectY - rectHeight / 2;
		playing = true;
		Image hiraganaImage = new Image(hiraganaImageName);
		final ImageView hiragana = new ImageView(hiraganaImage);
		hiragana.setScaleX(1.2);
		hiragana.setScaleY(1.2);

		hiragana.setLayoutX(width / 2 - hiragana.getLayoutBounds().getWidth()
				/ 2);
		hiragana.setLayoutY(height / 2 - hiragana.getLayoutBounds().getHeight()
				/ 2);

		root.getChildren().add(hiragana);

		// アニメーションを行なわせるパス
		// Rectangle path = new Rectangle(80, 80, 250, 200);
		Path path = new Path();
		// 枠線を越えないようにクリッピングする
		// path.setClip(new Rectangle(0, 0, width, height));

		MoveTo moveTo = new MoveTo();
		moveTo.setX(centerX);
		moveTo.setY(centerY);
		path.getElements().add(moveTo);

		boolean leftOfRight = random.nextInt() % 2 == 0;
		int negative = -1;
		if (leftOfRight) {
			negative = 1;
		}
		ArcTo arcTo = new ArcTo();
		arcTo.setX(centerX + 120 * negative);
		arcTo.setY(centerY);

		float radiusX = 50.0f + random.nextFloat() * 50.0f;
		float radiusY = 50.0f + random.nextFloat() * 50.0f;
		arcTo.setRadiusX(radiusX);
		arcTo.setRadiusY(radiusY);

		arcTo.setSweepFlag(leftOfRight);

		path.getElements().add(arcTo);

		// アニメーションの開始点に移動させる
		// hiragana.setTranslateX(80.0 - hiraganaImage.getWidth()/2.0);
		// hiragana.setTranslateY(80.0 - hiraganaImage.getHeight()/2.0);

		// 移動のアニメーション
		PathTransition transition = new PathTransition();
		// アニメーションの時間は4000ミリ秒
		transition.setDuration(Duration.millis(1_200L));
		// アニメーション対象の設定
		transition.setNode(hiragana);

		// アニメーションを行なわせるパス
		transition.setPath(path);
		// パスに沿って回転させる
		// transition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
		// 繰り返し回数の設定
		// transition.setCycleCount(2);

		// // 移動を行なうアニメーション
		// TranslateTransition transition = new TranslateTransition();
		// // アニメーション対象の設定
		// transition.setNode(hiragana);
		// // アニメーションの時間は4000ミリ秒
		// transition.setDuration(Duration.millis(4_000L + y));
		// // 開始位置の設定
		// transition.setFromX(0.0);
		// // 終了位置の設定
		// transition.setToX(300.0);
		// // 繰り返し回数の設定
		// transition.setCycleCount(2);
		// // アニメーションを反転させる
		// transition.setAutoReverse(true);

		// System.out.println(transition.getInterpolator());

		// ScaleTransition scale = new ScaleTransition(Duration.millis(4_000),
		// hiragana);
		// scale.setFromX(0.1);
		// scale.setFromY(0.1);
		// scale.setToX(2.0);
		// scale.setToY(2.0);
		//
		// FadeTransition fade = new FadeTransition(Duration.millis(3000),
		// hiragana);
		// fade.setFromValue(0.1);
		// fade.setToValue(1.0);

		// RotateTransition rotate = new
		// RotateTransition(Duration.millis(4_000),
		// hiragana);
		// rotate.setFromAngle(0.0);
		// rotate.setToAngle(1440.0);

		// ParallelTransition transition = new ParallelTransition(scale, fade);
		// transition.setCycleCount(1);
		// transition.setAutoReverse(false);

		transition.setOnFinished(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				System.out.println("on Animation Finished");
				root.getChildren().remove(hiragana);
				playing = false;
				currentAnimationCount--;
			}
		});

		transition.play();
	}

	/**
	 * シーングラフを作って、そのノードを返す。 このノードは40mmの色つき立方体を表示するシーン。ｚ軸を基準に20mm上に浮かせてる。
	 * 
	 * @return
	 */
	private Node createSceneGraph() {
		TransformGroup tg = new TransformGroup();
		Transform3D mt = new Transform3D();
		mt.setTranslation(new Vector3d(0.00, 0.0, 20 * 0.001));
		// 大きさ 40mmの色付き立方体を、Z軸上で20mm動かして配置）
		tg.setTransform(mt);
		tg.addChild(new ColorCube(20 * 0.001));
		return tg;
	}

	public void onWebcamOpen() {
		try {
			this.startCapture();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
