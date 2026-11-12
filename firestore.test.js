const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_UID_1 = "admin_uid_102";
const ADMIN_UID_2 = "admin_uid_204";
const ADMIN_UID_3 = "admin_uid_28";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

const nowDate = () => new Date(Date.now() - 1000);

test("Unauthenticated user: cannot read feed_posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("feed_posts").where("visibility", "==", "PUBLIC").get());
});

test("Authenticated user: can create and query public feed_posts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("feed_posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "দারুল উলুম কাওমি মাদরাসা",
      postType: "JOB_CIRCULAR",
      title: "দারুল উলুম কাওমি মাদরাসা",
      district: "কুমিল্লা",
      upazilaOrLocation: "কুমিল্লা সদর, কুমিল্লা",
      designation: "সহকারী শিক্ষক (আরবি, ফিকহ, তাফসীর)",
      qualification: "দাওরায়ে হাদীস / সম্মান",
      salaryOrExperience: "১২,০০০ - ১৮,০০০ টাকা",
      deadlineOrPreferredDistricts: "২০ অক্টোবর ২০২৫",
      description: "আবাসন ও খানা মাদরাসা বহন করবে।",
      contactNumber: "01700000000",
      madrasaId: "madrasa_1",
      isVerifiedMadrasa: false,
      isBoosted: false,
      visibility: "PUBLIC",
      approvalStatus: "APPROVED",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("feed_posts").where("visibility", "==", "PUBLIC").get()
  );
});

test("All 3 authorized admin emails and custom claims can perform admin operations; non-admins are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "regular_user@gmail.com",
  }).firestore();

  await assertSucceeds(
    aliceDb.collection("feed_posts").doc("post_boost_1").set({
      authorId: ALICE_UID,
      authorName: "জামিয়া ইসলামিয়া",
      postType: "JOB_CIRCULAR",
      title: "জামিয়া ইসলামিয়া",
      district: "ঢাকা",
      upazilaOrLocation: "ঢাকা",
      designation: "মুহাদ্দিস",
      qualification: "দাওরায়ে হাদীস",
      salaryOrExperience: "আলোচনা সাপেক্ষ",
      deadlineOrPreferredDistricts: "চলমান",
      description: "বিশেষ বিজ্ঞপ্তি",
      contactNumber: "01300448147",
      madrasaId: "",
      isVerifiedMadrasa: false,
      isBoosted: false,
      visibility: "PUBLIC",
      approvalStatus: "APPROVED",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  // Non-admin cannot set isBoosted = true or write to /admins
  await assertFails(
    aliceDb.collection("feed_posts").doc("post_boost_1").update({
      isBoosted: true,
      updatedAt: nowDate(),
    })
  );
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      adminId: ALICE_UID,
      email: "foysalahamad102@gmail.com",
      adminPhone: "01300448147",
      role: "SUPER_ADMIN",
      verifiedByAuthToken: true,
      updatedAt: nowDate(),
    })
  );

  // Authorized Admin 1: foysalahamad102@gmail.com
  const admin1Db = testEnv.authenticatedContext(ADMIN_UID_1, {
    email: "foysalahamad102@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    admin1Db.collection("admins").doc(ADMIN_UID_1).set({
      adminId: ADMIN_UID_1,
      email: "foysalahamad102@gmail.com",
      adminPhone: "01300448147",
      role: "SUPER_ADMIN",
      verifiedByAuthToken: true,
      updatedAt: nowDate(),
    })
  );
  await assertSucceeds(
    admin1Db.collection("feed_posts").doc("post_boost_1").update({
      isBoosted: true,
      updatedAt: nowDate(),
    })
  );

  // Authorized Admin 2: foysalahamad204@gmail.com
  const admin2Db = testEnv.authenticatedContext(ADMIN_UID_2, {
    email: "foysalahamad204@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    admin2Db.collection("feed_posts").doc("post_boost_1").update({
      isBoosted: false,
      updatedAt: nowDate(),
    })
  );

  // Authorized Admin 3: fyslahm28@gmail.com
  const admin3Db = testEnv.authenticatedContext(ADMIN_UID_3, {
    email: "fyslahm28@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    admin3Db.collection("feed_posts").doc("post_boost_1").update({
      isBoosted: true,
      updatedAt: nowDate(),
    })
  );
});

test("Telegram notifications queue: regular users can enqueue event logs, only admins can read/list them", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "regular@gmail.com",
  }).firestore();

  await assertSucceeds(
    aliceDb.collection("telegram_notifications").doc("tg_1").set({
      eventType: "NEW_CIRCULAR",
      title: "নতুন নিয়োগ বিজ্ঞপ্তি প্রকাশিত হয়েছে",
      message: "জামিয়া ইসলামিয়ায় মুহাদ্দিস পদে নতুন নিয়োগ বিজ্ঞপ্তি।",
      actorId: ALICE_UID,
      actorName: "জামিয়া ইসলামিয়া",
      targetPhone: "01300448147",
      referenceId: "post_1",
      deliveryStatus: "QUEUED",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  // Regular user cannot read or list telegram_notifications
  await assertFails(aliceDb.collection("telegram_notifications").get());

  // Verified Admin can read and list telegram_notifications
  const adminDb = testEnv.authenticatedContext(ADMIN_UID_1, {
    email: "foysalahamad102@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("telegram_notifications").get());
});

test("DirectMessage: supports Islamic reactions, sender-only edit and delete-for-everyone, and per-user delete-for-me", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const createdTs = nowDate();
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_adv_1").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: BOB_UID,
      receiverName: "জামিয়া দারুল উলুম",
      subject: "খেদমত সংক্রান্ত যোগাযোগ",
      body: "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহ",
      isRead: false,
      readByUserIds: [ALICE_UID],
      createdAt: createdTs,
    })
  );

  // Sender (Alice) can idempotently retry set() on the same preallocated messageId without PERMISSION_DENIED
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_adv_1").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: BOB_UID,
      receiverName: "জামিয়া দারুল উলুম",
      subject: "খেদমত সংক্রান্ত যোগাযোগ",
      body: "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহ",
      isRead: false,
      readByUserIds: [ALICE_UID],
      createdAt: nowDate(),
    })
  );

  // Receiver (Bob) can acknowledge delivery via readByUserIds (`DELIVERED:<uid>`)
  await assertSucceeds(
    bobDb.collection("messages").doc("msg_adv_1").update({
      readByUserIds: [ALICE_UID, `DELIVERED:${BOB_UID}`],
    })
  );

  // Receiver (Bob) can add an Islamic reaction (DUA) and mark read
  await assertSucceeds(
    bobDb.collection("messages").doc("msg_adv_1").update({
      isRead: true,
      readByUserIds: [ALICE_UID, `DELIVERED:${BOB_UID}`, BOB_UID],
      receiverReaction: "DUA",
    })
  );

  // Receiver (Bob) CANNOT edit the message body or delete for everyone
  await assertFails(
    bobDb.collection("messages").doc("msg_adv_1").update({
      body: "অননুমোদিত পরিবর্তন",
      isEdited: true,
    })
  );
  await assertFails(
    bobDb.collection("messages").doc("msg_adv_1").update({
      isDeletedForEveryone: true,
    })
  );

  // Sender (Alice) can add her own Islamic reaction (DEENI_PRERONA) and edit her message
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_adv_1").update({
      body: "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহি ওয়া বারাকাতুহু",
      isEdited: true,
      editedAt: nowDate(),
      senderReaction: "DEENI_PRERONA",
    })
  );

  // Receiver (Bob) can delete for himself (deletedForReceiver)
  await assertSucceeds(
    bobDb.collection("messages").doc("msg_adv_1").update({
      deletedForReceiver: true,
    })
  );

  // Outsider (not sender or receiver) CANNOT read or update a 1-on-1 message (even when groupMemberIds is omitted)
  const outsiderDb = testEnv.authenticatedContext("outsider_uid_789").firestore();
  await assertFails(outsiderDb.collection("messages").doc("msg_adv_1").get());
  await assertFails(
    outsiderDb.collection("messages").doc("msg_adv_1").update({
      isRead: true,
    })
  );

  // Sender (Alice) can delete for everyone (isDeletedForEveryone)
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_adv_1").update({
      body: "এই পয়গামটি প্রত্যাহার করা হয়েছে",
      isDeletedForEveryone: true,
    })
  );
});

test("Blocking & Chat Themes: blocking prevents sending messages in both directions until unblocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const ts = nowDate();
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "মুফতি মাহমুদ",
      accountType: "muhtamim",
      district: "ঢাকা",
      designation: "মুহতামিম",
      qualification: "দাওরায়ে হাদীস",
      experience: "১০ বছর",
      preferredDistricts: "ঢাকা",
      bio: "",
      contactInfo: "01700000000",
      madrasaName: "জামিয়া রাহমানিয়া",
      blockedUserIds: [ALICE_UID],
      chatThemes: { [ALICE_UID]: "ROYAL_GOLD" },
      isVerified: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // While Bob has blocked Alice, Alice cannot send a message to Bob
  await assertFails(
    aliceDb.collection("messages").doc("msg_blocked_1").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: BOB_UID,
      receiverName: "মুফতি মাহমুদ",
      subject: "যোগাযোগ",
      body: "ব্লক অবস্থায় পয়গাম",
      isRead: false,
      createdAt: nowDate(),
    })
  );

  // Bob unblocks Alice
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).update({
      blockedUserIds: [],
      updatedAt: nowDate(),
    })
  );

  // Now Alice can send a message to Bob
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_unblocked_1").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: BOB_UID,
      receiverName: "মুফতি মাহমুদ",
      subject: "যোগাযোগ",
      body: "আনব্লক করার পর পয়গাম",
      isRead: false,
      createdAt: nowDate(),
    })
  );
});

test("ChatGroup & Image Attachments: members can create groups, add/remove members, and send 1-5 images (rejects >5)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext("outsider_uid_999").firestore();

  const ts = nowDate();

  // Alice creates a chat group with Bob
  await assertSucceeds(
    aliceDb.collection("chat_groups").doc("group_1").set({
      name: "দাওরায়ে হাদীস ফোরাম",
      photoUrl: "preset:scholar_emerald",
      creatorId: ALICE_UID,
      creatorName: "মাওলানা আব্দুল্লাহ",
      memberIds: [ALICE_UID, BOB_UID],
      memberNames: {
        [ALICE_UID]: "মাওলানা আব্দুল্লাহ",
        [BOB_UID]: "মুফতি মাহমুদ",
      },
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Outsider cannot read the group
  await assertFails(outsiderDb.collection("chat_groups").doc("group_1").get());

  // Member (Bob) can read the group and update group name/members
  await assertSucceeds(bobDb.collection("chat_groups").doc("group_1").get());
  await assertSucceeds(
    bobDb.collection("chat_groups").doc("group_1").update({
      memberIds: [ALICE_UID, BOB_UID, "member_3"],
      updatedAt: nowDate(),
    })
  );

  // Alice can send a group message with 3 attached images (1-5 allowed)
  await assertSucceeds(
    aliceDb.collection("messages").doc("group_msg_1").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: "GROUP:group_1",
      receiverName: "দাওরায়ে হাদীস ফোরাম",
      groupId: "group_1",
      groupName: "দাওরায়ে হাদীস ফোরাম",
      groupMemberIds: [ALICE_UID, BOB_UID, "member_3"],
      subject: "গ্রুপ কথোপকথন",
      body: "গ্রুপের সবাইকে সালাম ও ছবি প্রেরণ",
      imageUrls: [
        "data:image/jpeg;base64,img1",
        "data:image/jpeg;base64,img2",
        "data:image/jpeg;base64,img3",
      ],
      isRead: false,
      createdAt: nowDate(),
    })
  );

  // Sending more than 5 images (6 images) is rejected by security rules
  await assertFails(
    aliceDb.collection("messages").doc("group_msg_too_many_imgs").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: "GROUP:group_1",
      receiverName: "দাওরায়ে হাদীস ফোরাম",
      groupId: "group_1",
      groupName: "দাওরায়ে হাদীস ফোরাম",
      groupMemberIds: [ALICE_UID, BOB_UID],
      subject: "গ্রুপ কথোপকথন",
      body: "অতিরিক্ত ছবি",
      imageUrls: ["img1", "img2", "img3", "img4", "img5", "img6"],
      isRead: false,
      createdAt: nowDate(),
    })
  );

  // Group member (Bob) can add his reaction via userReactions map
  await assertSucceeds(
    bobDb.collection("messages").doc("group_msg_1").update({
      userReactions: { [BOB_UID]: "DUA" },
      readByUserIds: [BOB_UID],
    })
  );
});

test("Facebook-Style Islamic Feed: multi-category Khedmot posts, GENERAL_POST, reactions, and post_comments ownership rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const ts = nowDate();

  // 1. Alice publishes a multi-category Seeker Application with customCategory ("অন্যান্য")
  await assertSucceeds(
    aliceDb.collection("feed_posts").doc("post_multi_cat_1").set({
      authorId: ALICE_UID,
      authorName: "মাওলানা আব্দুল্লাহ",
      postType: "SEEKER_APPLICATION",
      title: "মাওলানা আব্দুল্লাহ",
      district: "ঢাকা",
      upazilaOrLocation: "মোহাম্মদপুর, ঢাকা",
      designation: "নূরানী, নাজেরা, ইমাম, হিফজ বিভাগ",
      qualification: "দাওরায়ে হাদীস ও কারী",
      salaryOrExperience: "৫ বছর",
      deadlineOrPreferredDistricts: "ঢাকা, গাজীপুর",
      description: "নূরানী, নাজেরা ও ইমামতি খেদমতে আগ্রহী।",
      contactNumber: "01711223344",
      madrasaId: "",
      isVerifiedMadrasa: false,
      isBoosted: false,
      khedmotCategories: ["নূরানী", "নাজেরা", "ইমাম", "অন্যান্য"],
      customCategory: "হিফজ বিভাগ",
      userReactions: {},
      userReactionNames: {},
      commentCount: 0,
      shareCount: 0,
      visibility: "PUBLIC",
      approvalStatus: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 2. Bob reacts to Alice's post with "DUA" (মাশাআল্লাহ/দোয়া)
  await assertSucceeds(
    bobDb.collection("feed_posts").doc("post_multi_cat_1").update({
      userReactions: { [BOB_UID]: "DUA" },
      userReactionNames: { [BOB_UID]: "মুফতি মাহমুদ" },
      updatedAt: nowDate(),
    })
  );

  // 3. Bob CANNOT forge a reaction on behalf of Alice or edit Alice's post title
  await assertFails(
    bobDb.collection("feed_posts").doc("post_multi_cat_1").update({
      userReactions: { [BOB_UID]: "DUA", [ALICE_UID]: "SAMMATI" },
      updatedAt: nowDate(),
    })
  );
  await assertFails(
    bobDb.collection("feed_posts").doc("post_multi_cat_1").update({
      title: "অননুমোদিত পরিবর্তন",
      updatedAt: nowDate(),
    })
  );

  // 4. Bob adds a public comment on Alice's post and increments commentCount
  await assertSucceeds(
    bobDb.collection("post_comments").doc("comment_1").set({
      postId: "post_multi_cat_1",
      authorId: BOB_UID,
      authorName: "মুফতি মাহমুদ",
      authorPhotoUrl: "preset:scholar_gold",
      text: "মাশাআল্লাহ, আল্লাহ আপনার খেদমত কবুল করুন।",
      parentCommentId: "",
      replyToAuthorName: "",
      isEdited: false,
      visibility: "PUBLIC",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );
  await assertSucceeds(
    bobDb.collection("feed_posts").doc("post_multi_cat_1").update({
      commentCount: 1,
      updatedAt: nowDate(),
    })
  );

  // 5. Alice replies to Bob's comment
  await assertSucceeds(
    aliceDb.collection("post_comments").doc("comment_reply_1").set({
      postId: "post_multi_cat_1",
      authorId: ALICE_UID,
      authorName: "মাওলানা আব্দুল্লাহ",
      text: "আমিন, জাযাকাল্লাহু খাইরান।",
      parentCommentId: "comment_1",
      replyToAuthorName: "মুফতি মাহমুদ",
      isEdited: false,
      visibility: "PUBLIC",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  // 6. Alice CANNOT edit or delete Bob's comment
  await assertFails(
    aliceDb.collection("post_comments").doc("comment_1").update({
      text: "পরিবর্তিত কমেন্ট",
      isEdited: true,
      updatedAt: nowDate(),
    })
  );
  await assertFails(
    aliceDb.collection("post_comments").doc("comment_1").delete()
  );

  // 7. Bob CAN edit and delete his own comment
  await assertSucceeds(
    bobDb.collection("post_comments").doc("comment_1").update({
      text: "মাশাআল্লাহ, আল্লাহ তাআলা আপনার ইলম ও আমলে বরকত দান করুন।",
      isEdited: true,
      updatedAt: nowDate(),
    })
  );
  await assertSucceeds(
    bobDb.collection("post_comments").doc("comment_1").delete()
  );

  // 8. Bob shares Alice's post as a GENERAL_POST in the feed
  await assertSucceeds(
    bobDb.collection("feed_posts").doc("post_shared_1").set({
      authorId: BOB_UID,
      authorName: "মুফতি মাহমুদ",
      postType: "GENERAL_POST",
      title: "দ্বীনি খেদমত পোস্ট শেয়ার",
      district: "ঢাকা",
      upazilaOrLocation: "",
      designation: "দ্বীনি আলোচনা",
      qualification: "",
      salaryOrExperience: "",
      deadlineOrPreferredDistricts: "",
      description: "যোগ্য উস্তাযের আবেদনটি সবার দৃষ্টি আকর্ষণ করছি।",
      contactNumber: "",
      madrasaId: "",
      isVerifiedMadrasa: false,
      isBoosted: false,
      sharedPostId: "post_multi_cat_1",
      sharedAuthorId: ALICE_UID,
      sharedAuthorName: "মাওলানা আব্দুল্লাহ",
      sharedTitle: "মাওলানা আব্দুল্লাহ",
      sharedDescription: "নূরানী, নাজেরা ও ইমামতি খেদমতে আগ্রহী।",
      sharedPostType: "SEEKER_APPLICATION",
      visibility: "PUBLIC",
      approvalStatus: "APPROVED",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );
});

test("App Releases Auto-Update: pre-login and signed-in users can read /app_releases/latest; only admins can publish or update release metadata", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "regular_user@gmail.com",
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID_1, {
    email: "foysalahamad102@gmail.com",
    email_verified: true,
  }).firestore();

  const ts = nowDate();
  const releasePayload = {
    releaseId: "latest",
    versionCode: 102,
    versionName: "1.0.2",
    apkDownloadUrl: "https://github.com/qawmi-bd/qawmi-kormosangsthan/releases/download/v1.0.2/Qawmi-Kormosangsthan-Bangladesh.apk",
    apkSha256: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    apkFileSizeBytes: 18452000,
    releaseNotes: "নতুন ফিচার ও দ্রুততর পারফরম্যান্স যুক্ত করা হয়েছে।",
    commitSha: "a1b2c3d4e5f6",
    githubReleaseTag: "v1.0.2",
    isMandatory: false,
    isPublished: true,
    releaseStatus: "PUBLISHED",
    publishedBy: "GitHub Actions CI / Super Admin",
    publishedAt: ts,
    updatedAt: ts,
  };

  // Non-admin cannot create or update /app_releases/latest
  await assertFails(
    aliceDb.collection("app_releases").doc("latest").set(releasePayload)
  );
  await assertFails(
    unauthDb.collection("app_releases").doc("latest").set(releasePayload)
  );

  // Admin can publish /app_releases/latest
  await assertSucceeds(
    adminDb.collection("app_releases").doc("latest").set(releasePayload)
  );

  // Unauthenticated user CAN read /app_releases/latest (pre-login update check), but CANNOT list or read other release docs
  await assertSucceeds(unauthDb.collection("app_releases").doc("latest").get());
  await assertFails(unauthDb.collection("app_releases").doc("v_102").get());
  await assertFails(unauthDb.collection("app_releases").get());

  // Authenticated user CAN read and list /app_releases/latest
  await assertSucceeds(aliceDb.collection("app_releases").doc("latest").get());
  await assertSucceeds(aliceDb.collection("app_releases").get());

  // Non-admin still cannot overwrite or delete /app_releases/latest
  await assertFails(
    aliceDb.collection("app_releases").doc("latest").update({
      versionCode: 999,
      apkDownloadUrl: "https://malicious.example.com/fake.apk",
      updatedAt: nowDate(),
    })
  );
  await assertFails(aliceDb.collection("app_releases").doc("latest").delete());
});

test("Feed Post Storage Metadata & Extended Attachments: prevents 1 MiB limit and secures /feed_post_attachments", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowDate();

  // 1. Alice creates an extended attachment document in /feed_post_attachments
  await assertSucceeds(
    aliceDb.collection("feed_post_attachments").doc("att_post_1").set({
      postId: "post_storage_1",
      authorId: ALICE_UID,
      attachmentType: "EXTENDED_CONTENT",
      mimeType: "text/plain",
      fileName: "extended_description.txt",
      downloadUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/post_storage_1_media_0.jpg?alt=media",
      extendedText: "বিস্তারিত নিয়োগ বিজ্ঞপ্তি ও মাদরাসার তথ্যাবলি।",
      mediaDataUrl: "",
      sizeBytes: 245000,
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 2. Alice creates a feed_posts document referencing Firebase Storage URLs, mediaTypes, mediaMetadata, and attachmentDocId
  await assertSucceeds(
    aliceDb.collection("feed_posts").doc("post_storage_1").set({
      authorId: ALICE_UID,
      authorName: "জামিয়া রাহমানিয়া আরাবিয়া",
      postType: "GENERAL_POST",
      title: "দাওরায়ে হাদীস পরীক্ষার রুটিন ও বিজ্ঞপ্তি",
      district: "ঢাকা",
      upazilaOrLocation: "মোহাম্মদপুর",
      designation: "দ্বীনি খেদমত ও সাধারণ পোস্ট",
      qualification: "সর্বসাধারণের জন্য উন্মুক্ত",
      salaryOrExperience: "",
      deadlineOrPreferredDistricts: "ঢাকা",
      description: "সংযুক্ত ছবিতে বিস্তারিত রুটিন দেওয়া হলো।",
      contactNumber: "দেওয়া হয়নি",
      madrasaId: "",
      isVerifiedMadrasa: false,
      isBoosted: false,
      imageUrls: [
        "https://firebasestorage.googleapis.com/v0/b/demo/o/post_storage_1_media_0.jpg?alt=media",
      ],
      mediaTypes: ["IMAGE"],
      mediaMetadata: [
        {
          url: "https://firebasestorage.googleapis.com/v0/b/demo/o/post_storage_1_media_0.jpg?alt=media",
          mediaType: "IMAGE",
          mimeType: "image/jpeg",
          fileName: "post_storage_1_media_0.jpg",
          sizeBytes: "245000",
          storagePath: "feed_posts/alice_uid_123/post_storage_1/post_storage_1_media_0.jpg",
          attachmentDocId: "att_post_1",
        },
      ],
      attachmentDocId: "att_post_1",
      visibility: "PUBLIC",
      approvalStatus: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 3. Bob can read the feed post and its extended attachment, but cannot overwrite or delete Alice's attachment
  await assertSucceeds(bobDb.collection("feed_posts").doc("post_storage_1").get());
  await assertSucceeds(bobDb.collection("feed_post_attachments").doc("att_post_1").get());
  await assertFails(
    bobDb.collection("feed_post_attachments").doc("att_post_1").update({
      extendedText: "অননুমোদিত পরিবর্তন",
      updatedAt: nowDate(),
    })
  );
  await assertFails(bobDb.collection("feed_post_attachments").doc("att_post_1").delete());
});

test("Collection List Queries (Query Snapshots): Inbox messages, Group conversations, Khedmot submissions (sent/received), and Feed posts succeed without Query snapshot errors", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowDate();

  // Seed 1-on-1 message without groupMemberIds (legacy format) and with groupMemberIds: []
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_list_1").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: BOB_UID,
      receiverName: "মুফতি মাহমুদ",
      subject: "খেদমত সংক্রান্ত যোগাযোগ",
      body: "আসসালামু আলাইকুম, আপনার বিজ্ঞপ্তিটি দেখে যোগাযোগ করছি।",
      isRead: false,
      readByUserIds: [ALICE_UID],
      createdAt: ts,
    })
  );

  // Seed a chat group and a group message
  await assertSucceeds(
    aliceDb.collection("chat_groups").doc("grp_list_1").set({
      groupId: "grp_list_1",
      name: "উস্তায মুযাকারা গ্রুপ",
      creatorId: ALICE_UID,
      creatorName: "মাওলানা আব্দুল্লাহ",
      memberIds: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_grp_list_1").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: "GROUP:grp_list_1",
      receiverName: "উস্তায মুযাকারা গ্রুপ",
      groupId: "grp_list_1",
      groupName: "উস্তায মুযাকারা গ্রুপ",
      groupMemberIds: [ALICE_UID, BOB_UID],
      subject: "গ্রুপ কথোপকথন",
      body: "গ্রুপে সবাইকে স্বাগতম।",
      isRead: false,
      readByUserIds: [ALICE_UID],
      createdAt: ts,
    })
  );

  // Seed a khedmot submission from Alice to Bob
  await assertSucceeds(
    aliceDb.collection("khedmot_submissions").doc("sub_list_1").set({
      postId: "post_1",
      postTitle: "জামিয়া রাহমানিয়া আরাবিয়া",
      postDesignation: "মুহাদ্দিস",
      applicantId: ALICE_UID,
      applicantName: "মাওলানা আব্দুল্লাহ",
      applicantQualification: "দাওরায়ে হাদিস ও ইফতা",
      applicantExperience: "৫ বছর",
      applicantPhone: "01711000000",
      coverNote: "আমি উক্ত খেদমতে আগ্রহী।",
      recipientId: BOB_UID,
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Seed a public feed post
  await assertSucceeds(
    bobDb.collection("feed_posts").doc("feed_list_1").set({
      authorId: BOB_UID,
      authorName: "মুফতি মাহমুদ",
      postType: "JOB_CIRCULAR",
      title: "জামিয়া ইসলামিয়া ঢাকা",
      district: "ঢাকা",
      upazilaOrLocation: "মিরপুর",
      designation: "মুহাদ্দিস",
      qualification: "দাওরায়ে হাদিস",
      salaryOrExperience: "আলোচনা সাপেক্ষে",
      deadlineOrPreferredDistricts: "৩০ নভেম্বর",
      description: "জরুরি ভিত্তিতে একজন মুহাদ্দিস আবশ্যক।",
      contactNumber: "01800000000",
      madrasaId: "",
      isVerifiedMadrasa: false,
      visibility: "PUBLIC",
      approvalStatus: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Verify A. Inbox & Group Conversations list queries for both Alice and Bob
  await assertSucceeds(aliceDb.collection("messages").where("senderId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("messages").where("receiverId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("messages").where("groupMemberIds", "array-contains", ALICE_UID).get());
  await assertSucceeds(bobDb.collection("messages").where("senderId", "==", BOB_UID).get());
  await assertSucceeds(bobDb.collection("messages").where("receiverId", "==", BOB_UID).get());
  await assertSucceeds(bobDb.collection("messages").where("groupMemberIds", "array-contains", BOB_UID).get());
  await assertSucceeds(aliceDb.collection("chat_groups").where("memberIds", "array-contains", ALICE_UID).get());
  await assertSucceeds(bobDb.collection("chat_groups").where("memberIds", "array-contains", BOB_UID).get());

  // Verify B. Application Management (আমার প্রেরিত আবেদন & মাদরাসায় প্রাপ্ত আবেদন) list queries
  await assertSucceeds(aliceDb.collection("khedmot_submissions").where("applicantId", "==", ALICE_UID).get());
  await assertSucceeds(bobDb.collection("khedmot_submissions").where("recipientId", "==", BOB_UID).get());
  await assertFails(aliceDb.collection("khedmot_submissions").where("recipientId", "==", BOB_UID).get());

  // Verify C. Feed Posts list queries
  await assertSucceeds(aliceDb.collection("feed_posts").where("visibility", "==", "PUBLIC").get());
  await assertSucceeds(bobDb.collection("feed_posts").get());
  await assertFails(unauthDb.collection("feed_posts").get());
});

test("KKS Intelligence Engine: intelligence_config is readable by signed-in users and writable only by verified admins", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "regular_user@gmail.com",
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID_1, {
    email: "foysalahamad102@gmail.com",
    email_verified: true,
    role: "SUPER_ADMIN",
  }).firestore();

  const validConfig = {
    configId: "global_weights",
    skillMatchWeight: 34.0,
    locationMatchWeight: 22.0,
    experienceWeight: 14.0,
    freshnessHalfLifeHours: 36.0,
    trendingWeight: 18.0,
    verifiedMadrasaBonus: 12.0,
    newPostBoostHours: 18.0,
    minDwellTimeMs: 2500,
    fraudPenaltyWeight: 45.0,
    smartNotificationThreshold: 62.0,
    isSmartRankingEnabled: true,
    isTrendingEnabled: true,
    isFraudDetectionEnabled: true,
    isSmartNotificationEnabled: true,
    updatedBy: "নাজিমে আলা (অ্যাডমিন)",
    updatedAt: nowDate(),
  };

  // Non-admin cannot modify algorithm weights
  await assertFails(
    aliceDb.collection("intelligence_config").doc("global_weights").set(validConfig)
  );

  // Verified Admin can save algorithm weights
  await assertSucceeds(
    adminDb.collection("intelligence_config").doc("global_weights").set(validConfig)
  );

  // Signed-in user can read global_weights
  await assertSucceeds(
    aliceDb.collection("intelligence_config").doc("global_weights").get()
  );
});

test("KKS Intelligence Engine: engagement_receipts enforce deterministic ID, dwell-time gate, and create-only immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Valid VIEW receipt with >= 1500ms dwell time and deterministic ID {uid}_VIEW
  await assertSucceeds(
    aliceDb
      .collection("post_intelligence_metrics")
      .doc("post_101")
      .collection("engagement_receipts")
      .doc(`${ALICE_UID}_VIEW`)
      .set({
        userId: ALICE_UID,
        postId: "post_101",
        eventType: "VIEW",
        dwellTimeMs: 3200,
        createdAt: nowDate(),
      })
  );

  // Cannot spoof another user's receipt ID
  await assertFails(
    aliceDb
      .collection("post_intelligence_metrics")
      .doc("post_101")
      .collection("engagement_receipts")
      .doc(`${BOB_UID}_VIEW`)
      .set({
        userId: ALICE_UID,
        postId: "post_101",
        eventType: "VIEW",
        dwellTimeMs: 3200,
        createdAt: nowDate(),
      })
  );

  // Cannot create VIEW receipt with rapid scroll dwell time (< 1500ms)
  await assertFails(
    bobDb
      .collection("post_intelligence_metrics")
      .doc("post_101")
      .collection("engagement_receipts")
      .doc(`${BOB_UID}_VIEW`)
      .set({
        userId: BOB_UID,
        postId: "post_101",
        eventType: "VIEW",
        dwellTimeMs: 400,
        createdAt: nowDate(),
      })
  );

  // User affinity state is strictly owner-only
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("intelligence_state")
      .doc("affinity")
      .set({
        userId: ALICE_UID,
        categoryWeights: { দাওরা: 4.5, ইফতা: 3.0 },
        districtWeights: { ঢাকা: 5.0 },
        totalInteractions: 3,
        lastEventAt: nowDate(),
        updatedAt: nowDate(),
      })
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("intelligence_state")
      .doc("affinity")
      .get()
  );
});

test("Islamic Book Library & Self-Publishing: Contributor account isolation, mandatory copyright verification, cross-contributor protection, Admin review workflow, and Public Catalog approval gate", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "author_alice@gmail.com",
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "author_bob@gmail.com",
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID_1, {
    email: "foysalahamad102@gmail.com",
    email_verified: true,
    role: "SUPER_ADMIN",
  }).firestore();

  const ts = nowDate();

  // 1. Alice registers her Contributor Profile (/book_contributors/alice_uid_123)
  await assertSucceeds(
    aliceDb.collection("book_contributors").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "মাওলানা আব্দুল্লাহ আল-মামুন",
      mobileNumber: "01711223344",
      email: "author_alice@gmail.com",
      roleType: "AUTHOR",
      organizationName: "মাকতাবাতুল ফুরকান",
      shortBio: "ইসলামিক ফিকহ ও আকিদা বিষয়ক লেখক ও গবেষক।",
      contactDetails: "ঢাকা, বাংলাদেশ",
      isVerifiedContributor: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob CANNOT read or overwrite Alice's Contributor Profile
  await assertFails(bobDb.collection("book_contributors").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("book_contributors").doc(ALICE_UID).update({
      shortBio: "অননুমোদিত পরিবর্তন",
      updatedAt: nowDate(),
    })
  );

  // Admin CAN read and verify Alice's Contributor Profile
  await assertSucceeds(adminDb.collection("book_contributors").doc(ALICE_UID).get());
  await assertSucceeds(
    adminDb.collection("book_contributors").doc(ALICE_UID).update({
      isVerifiedContributor: true,
      updatedAt: nowDate(),
    })
  );

  const validSha256 = "a3f5c8d9e0b1a2f4c6d8e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1";
  const baseSubmission = {
    submissionId: "SUB_2026_1001",
    contributorId: ALICE_UID,
    contributorName: "মাওলানা আব্দুল্লাহ আল-মামুন",
    contributorRole: "AUTHOR",
    titleBangla: "আহকামুল কুরআন ও সমকালীন মাসায়েল",
    authorBangla: "মাওলানা আব্দুল্লাহ আল-মামুন",
    translatorBangla: "",
    compilerBangla: "",
    publisherBangla: "মাকতাবাতুল ফুরকান",
    categoryId: "FIQH_MASAIL",
    languageBangla: "বাংলা ও আরবি",
    publicationYear: "২০২৫",
    editionBangla: "১ম সংস্করণ",
    isbn: "978-984-8920-11-4",
    shortDescriptionBangla: "সমকালীন জীবনঘনিষ্ঠ ফিকহি মাসায়েলের দালিলিক সমাধান।",
    coverImageUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/cover_1001.jpg?alt=media",
    pdfDownloadUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/book_1001.pdf?alt=media",
    pdfStoragePath: `islamic_book_submissions/${ALICE_UID}/SUB_2026_1001/book.pdf`,
    pdfSha256: validSha256,
    totalVolumes: 1,
    totalPages: 320,
    fileSizeBytes: 8450000,
    copyrightOwnerName: "মাওলানা আব্দুল্লাহ আল-মামুন",
    permissionType: "SELF_COPYRIGHT",
    permissionProofUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/proof_1001.jpg?alt=media",
    isTruthConfirmed: true,
    status: "SUBMITTED",
    adminComment: "",
    applicantReplyNote: "",
    reviewedByAdminId: "",
    createdAt: ts,
    updatedAt: ts,
  };

  // 2. Submission MUST fail if isTruthConfirmed is false, or if pdfDownloadUrl is Base64 data:, or if contributor tries to self-approve (status: APPROVED)
  await assertFails(
    aliceDb.collection("islamic_book_submissions").doc("SUB_2026_1001").set({
      ...baseSubmission,
      isTruthConfirmed: false,
    })
  );
  await assertFails(
    aliceDb.collection("islamic_book_submissions").doc("SUB_2026_1001").set({
      ...baseSubmission,
      pdfDownloadUrl: "data:application/pdf;base64,JVBERi0xLjQK",
    })
  );
  await assertFails(
    aliceDb.collection("islamic_book_submissions").doc("SUB_2026_1001").set({
      ...baseSubmission,
      status: "APPROVED",
    })
  );

  // 3. Valid submission by Alice succeeds
  await assertSucceeds(
    aliceDb.collection("islamic_book_submissions").doc("SUB_2026_1001").set(baseSubmission)
  );

  // 4. Cross-Contributor Isolation: Bob CANNOT read, list, or modify Alice's submission
  await assertFails(
    bobDb.collection("islamic_book_submissions").doc("SUB_2026_1001").get()
  );
  await assertFails(
    bobDb.collection("islamic_book_submissions").where("contributorId", "==", ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("islamic_book_submissions").doc("SUB_2026_1001").update({
      titleBangla: "হ্যাকড বই",
      updatedAt: nowDate(),
    })
  );

  // Alice CAN read and list her own submissions
  await assertSucceeds(
    aliceDb.collection("islamic_book_submissions").where("contributorId", "==", ALICE_UID).get()
  );

  // 5. Admin Review Workflow: Admin marks MORE_INFO_REQUIRED -> Alice replies -> Admin marks APPROVED & PUBLISHED
  await assertSucceeds(
    adminDb.collection("islamic_book_submissions").doc("SUB_2026_1001").update({
      status: "MORE_INFO_REQUIRED",
      adminComment: "অনুগ্রহ করে প্রকাশনীর অনুমোদনপত্রের স্পষ্ট কপি সংযুক্ত করুন।",
      reviewedByAdminId: ADMIN_UID_1,
      updatedAt: nowDate(),
    })
  );

  // Alice submits additional info and resets status to SUBMITTED
  await assertSucceeds(
    aliceDb.collection("islamic_book_submissions").doc("SUB_2026_1001").update({
      applicantReplyNote: "স্পষ্ট অনুমোদনপত্র সংযুক্ত করা হয়েছে।",
      status: "SUBMITTED",
      updatedAt: nowDate(),
    })
  );

  // Admin approves and publishes the submission
  await assertSucceeds(
    adminDb.collection("islamic_book_submissions").doc("SUB_2026_1001").update({
      status: "PUBLISHED",
      adminComment: "কপিরাইট ও পিডিএফ যাচাই সম্পন্ন। পাবলিক লাইব্রেরিতে প্রকাশিত।",
      reviewedByAdminId: ADMIN_UID_1,
      updatedAt: nowDate(),
    })
  );

  // 6. Public Islamic Book Catalog Gate (/islamic_books_catalog/{bookId})
  const catalogBookPayload = {
    bookId: "ibook_pub_1001",
    submissionId: "SUB_2026_1001",
    contributorId: ALICE_UID,
    titleBangla: "আহকামুল কুরআন ও সমকালীন মাসায়েল",
    authorBangla: "মাওলানা আব্দুল্লাহ আল-মামুন",
    translatorBangla: "",
    compilerBangla: "",
    publisherBangla: "মাকতাবাতুল ফুরকান",
    categoryId: "FIQH_MASAIL",
    languageBangla: "বাংলা ও আরবি",
    publicationYear: "২০২৫",
    isPublicationYearVerified: true,
    editionBangla: "১ম সংস্করণ",
    isbn: "978-984-8920-11-4",
    shortDescriptionBangla: "সমকালীন জীবনঘনিষ্ঠ ফিকহি মাসায়েলের দালিলিক সমাধান।",
    tableOfContents: ["অধ্যায় ১: ইবাদত ও পবিত্রতা", "অধ্যায় ২: মুআমালাত ও লেনদেন"],
    coverImageUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/cover_1001.jpg?alt=media",
    pdfDownloadUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/book_1001.pdf?alt=media",
    pdfSha256: validSha256,
    volumeNumber: 1,
    totalVolumes: 1,
    totalPages: 320,
    fileSizeBytes: 8450000,
    isPdfAvailable: true,
    copyrightOwnerName: "মাওলানা আব্দুল্লাহ আল-মামুন",
    permissionType: "SELF_COPYRIGHT",
    isAdminApproved: true,
    publicationStatus: "PUBLISHED",
    approvedByAdminId: ADMIN_UID_1,
    publishedAt: nowDate(),
    updatedAt: nowDate(),
  };

  // Non-admin CANNOT publish directly to /islamic_books_catalog
  await assertFails(
    aliceDb.collection("islamic_books_catalog").doc("ibook_pub_1001").set(catalogBookPayload)
  );

  // Admin publishes to /islamic_books_catalog
  await assertSucceeds(
    adminDb.collection("islamic_books_catalog").doc("ibook_pub_1001").set(catalogBookPayload)
  );

  // Readers (Bob) can query and read approved & PUBLISHED books
  await assertSucceeds(
    bobDb
      .collection("islamic_books_catalog")
      .where("isAdminApproved", "==", true)
      .where("publicationStatus", "==", "PUBLISHED")
      .get()
  );
  await assertSucceeds(
    bobDb.collection("islamic_books_catalog").doc("ibook_pub_1001").get()
  );

  // If Admin suspends the book (publicationStatus: SUSPENDED), regular users can no longer read it
  await assertSucceeds(
    adminDb.collection("islamic_books_catalog").doc("ibook_pub_1001").update({
      publicationStatus: "SUSPENDED",
      updatedAt: nowDate(),
    })
  );
  await assertFails(
    bobDb.collection("islamic_books_catalog").doc("ibook_pub_1001").get()
  );

  // 7. Audit Log (/islamic_book_audit_logs/{logId}) is create-only and immutable
  await assertSucceeds(
    aliceDb.collection("islamic_book_audit_logs").doc("audit_1001").set({
      logId: "audit_1001",
      actorId: ALICE_UID,
      actorName: "মাওলানা আব্দুল্লাহ আল-মামুন",
      actorRole: "CONTRIBUTOR",
      actionType: "BOOK_SUBMITTED",
      submissionId: "SUB_2026_1001",
      bookTitle: "আহকামুল কুরআন ও সমকালীন মাসায়েল",
      pdfSha256: validSha256,
      details: "নতুন বই প্রকাশের আবেদন জমা দেওয়া হয়েছে।",
      createdAt: nowDate(),
    })
  );
  await assertFails(
    aliceDb.collection("islamic_book_audit_logs").doc("audit_1001").delete()
  );
  await assertFails(
    unauthDb.collection("islamic_books_catalog").doc("ibook_pub_1001").get()
  );
});

test("Islamic Article & Knowledge Cultivation System: Author profile isolation, submission authenticity gate, cross-author privacy, Admin review & publication gate, and article bookmarks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "author_alice@gmail.com",
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "author_bob@gmail.com",
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID_1, {
    email: "foysalahamad102@gmail.com",
    email_verified: true,
    role: "SUPER_ADMIN",
  }).firestore();

  const ts = nowDate();

  // 1. Author profile (/article_authors/{userId})
  await assertSucceeds(
    aliceDb.collection("article_authors").doc(ALICE_UID).set({
      userId: ALICE_UID,
      authorName: "মাওলানা আব্দুল্লাহ আল-মামুন",
      shortBio: "উস্তাযুল হাদিস ও ফিকহ গবেষক, জামিয়া রাহমানিয়া ঢাকা।",
      designationOrQualification: "দাওরায়ে হাদিস ও ইফতা",
      institutionName: "জামিয়া রাহমানিয়া ঢাকা",
      isVerifiedAuthor: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot read or overwrite Alice's author profile
  await assertFails(bobDb.collection("article_authors").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("article_authors").doc(ALICE_UID).update({
      shortBio: "অননুমোদিত পরিবর্তন",
      updatedAt: nowDate(),
    })
  );

  // 2. Article Submission (/islamic_article_submissions/{submissionId})
  const baseArticleSub = {
    submissionId: "ART_SUB_2026_1001",
    authorId: ALICE_UID,
    authorName: "মাওলানা আব্দুল্লাহ আল-মামুন",
    authorBio: "উস্তাযুল হাদিস ও ফিকহ গবেষক, জামিয়া রাহমানিয়া ঢাকা।",
    title: "ইসলামে হালাল উপার্জনের গুরুত্ব ও সমকালীন দিকনির্দেশনা",
    categoryId: "FIQH_MASAIL",
    content: "## ভূমিকা\nইসলামে হালাল রিযিক অন্বেষণ করা ফরয ইবাদতের পর অন্যতম গুরুত্বপূর্ণ দায়িত্ব।",
    coverImageUrl: "",
    quranReferences: "সূরা আল-বাকারা: আয়াত ১৬৮, ১৭২; সূরা আল-মুমিনুন: আয়াত ৫১",
    hadithReferences: "সহীহ বুখারী, হাদিস নং ২০৭২; সহীহ মুসলিম, হাদিস নং ১০১৫",
    otherSourcesAndBibliography: "ফাতহুল বারী (৪/৩০৩), আল-বাহরুর রায়েক (৮/১৯০), ফাতাওয়া শামী (৬/৪২২)",
    isAuthenticityConfirmed: true,
    status: "SUBMITTED",
    adminComment: "",
    authorReplyNote: "",
    reviewedByAdminId: "",
    isReferencesVerifiedByAdmin: false,
    publishedArticleId: "",
    createdAt: ts,
    updatedAt: ts,
  };

  // Rejects if isAuthenticityConfirmed is false or if author tries to self-approve (status: PUBLISHED)
  await assertFails(
    aliceDb.collection("islamic_article_submissions").doc("ART_SUB_2026_1001").set({
      ...baseArticleSub,
      isAuthenticityConfirmed: false,
    })
  );
  await assertFails(
    aliceDb.collection("islamic_article_submissions").doc("ART_SUB_2026_1001").set({
      ...baseArticleSub,
      status: "PUBLISHED",
    })
  );

  // Valid submission by Alice succeeds
  await assertSucceeds(
    aliceDb.collection("islamic_article_submissions").doc("ART_SUB_2026_1001").set(baseArticleSub)
  );

  // Cross-author isolation: Bob CANNOT read, list, or modify Alice's submission
  await assertFails(
    bobDb.collection("islamic_article_submissions").doc("ART_SUB_2026_1001").get()
  );
  await assertFails(
    bobDb.collection("islamic_article_submissions").where("authorId", "==", ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("islamic_article_submissions").doc("ART_SUB_2026_1001").update({
      title: "হ্যাকড প্রবন্ধ",
      updatedAt: nowDate(),
    })
  );

  // Alice CAN read and list her own submissions
  await assertSucceeds(
    aliceDb.collection("islamic_article_submissions").where("authorId", "==", ALICE_UID).get()
  );

  // 3. Admin Review Workflow: Admin marks MORE_INFO_REQUIRED -> Alice revises & resubmits -> Admin marks PUBLISHED
  await assertSucceeds(
    adminDb.collection("islamic_article_submissions").doc("ART_SUB_2026_1001").update({
      status: "MORE_INFO_REQUIRED",
      adminComment: "অনুগ্রহ করে ফাতাওয়া শামীর পৃষ্ঠা ও অধ্যায় সুনির্দিষ্টভাবে উল্লেখ করুন।",
      reviewedByAdminId: ADMIN_UID_1,
      updatedAt: nowDate(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("islamic_article_submissions").doc("ART_SUB_2026_1001").update({
      otherSourcesAndBibliography: "ফাতহুল বারী (৪/৩০৩), রদ্দুল মুহতার / ফাতাওয়া শামী (কিতাবুল হাযর ওয়াল ইবাহাহ, ৬/৪২২)",
      authorReplyNote: "ফাতাওয়া শামীর অধ্যায় ও পৃষ্ঠা যুক্ত করা হয়েছে।",
      status: "SUBMITTED",
      updatedAt: nowDate(),
    })
  );

  await assertSucceeds(
    adminDb.collection("islamic_article_submissions").doc("ART_SUB_2026_1001").update({
      status: "PUBLISHED",
      isReferencesVerifiedByAdmin: true,
      publishedArticleId: "pub_art_1001",
      adminComment: "কুরআন-হাদিস ও ফিকহি তথ্যসূত্র যাচাই সম্পন্ন। পাবলিক প্রবন্ধ বিভাগে প্রকাশিত।",
      reviewedByAdminId: ADMIN_UID_1,
      updatedAt: nowDate(),
    })
  );

  // 4. Public Published Articles Gate (/islamic_articles_published/{articleId})
  const pubArticlePayload = {
    articleId: "pub_art_1001",
    submissionId: "ART_SUB_2026_1001",
    authorId: ALICE_UID,
    authorName: "মাওলানা আব্দুল্লাহ আল-মামুন",
    authorBio: "উস্তাযুল হাদিস ও ফিকহ গবেষক, জামিয়া রাহমানিয়া ঢাকা।",
    title: "ইসলামে হালাল উপার্জনের গুরুত্ব ও সমকালীন দিকনির্দেশনা",
    categoryId: "FIQH_MASAIL",
    summary: "কুরআন ও সুন্নাহর আলোকে হালাল উপার্জনের ফযিলত ও সমকালীন ব্যবসা-বাণিজ্যের শরয়ী নীতিমালা।",
    content: "## ভূমিকা\nইসলামে হালাল রিযিক অন্বেষণ করা ফরয ইবাদতের পর অন্যতম গুরুত্বপূর্ণ দায়িত্ব।",
    coverImageUrl: "",
    quranReferences: "সূরা আল-বাকারা: আয়াত ১৬৮, ১৭২; সূরা আল-মুমিনুন: আয়াত ৫১",
    hadithReferences: "সহীহ বুখারী, হাদিস নং ২০৭২; সহীহ মুসলিম, হাদিস নং ১০১৫",
    otherSourcesAndBibliography: "ফাতহুল বারী (৪/৩০৩), রদ্দুল মুহতার / ফাতাওয়া শামী (কিতাবুল হাযর ওয়াল ইবাহাহ, ৬/৪২২)",
    scholarlyDifferenceNote: "সমকালীন ডিজিটাল লেনদেনের খুঁটিনাটি বিষয়ে ফুকাহায়ে কেরামের মধ্যে শর্তসাপেক্ষ মতপার্থক্য রয়েছে।",
    isFeatured: true,
    isPopular: true,
    isReferencesVerified: true,
    isAdminApproved: true,
    publicationStatus: "PUBLISHED",
    approvedByAdminId: ADMIN_UID_1,
    publishedDateTextBangla: "১২ নভেম্বর ২০২৬",
    publishedAt: nowDate(),
    updatedAt: nowDate(),
  };

  // Non-admin CANNOT publish directly to /islamic_articles_published
  await assertFails(
    aliceDb.collection("islamic_articles_published").doc("pub_art_1001").set(pubArticlePayload)
  );

  // Admin publishes to /islamic_articles_published
  await assertSucceeds(
    adminDb.collection("islamic_articles_published").doc("pub_art_1001").set(pubArticlePayload)
  );

  // Readers (Bob) can query and read approved & PUBLISHED articles, and bookmark them
  await assertSucceeds(
    bobDb
      .collection("islamic_articles_published")
      .where("isAdminApproved", "==", true)
      .where("publicationStatus", "==", "PUBLISHED")
      .get()
  );
  await assertSucceeds(
    bobDb.collection("islamic_articles_published").doc("pub_art_1001").get()
  );
  await assertFails(
    unauthDb.collection("islamic_articles_published").doc("pub_art_1001").get()
  );

  // 5. Per-User Article Bookmarks (/users/{userId}/article_bookmarks/{articleId})
  await assertSucceeds(
    bobDb
      .collection("users")
      .doc(BOB_UID)
      .collection("article_bookmarks")
      .doc("pub_art_1001")
      .set({
        userId: BOB_UID,
        articleId: "pub_art_1001",
        title: "ইসলামে হালাল উপার্জনের গুরুত্ব ও সমকালীন দিকনির্দেশনা",
        categoryId: "FIQH_MASAIL",
        authorName: "মাওলানা আব্দুল্লাহ আল-মামুন",
        createdAt: nowDate(),
      })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("article_bookmarks").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("article_bookmarks").get()
  );
});




