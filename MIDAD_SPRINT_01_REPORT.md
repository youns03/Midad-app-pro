# Midad — Product Discovery, Full Audit & Development Sprint 01

**الحالة:** Sprint 01 مكتمل من ناحية التدقيق والتنفيذ والتحقق البنائي، مع قيود تشغيلية موثقة.

**الفرع:** `manus/product-audit-sprint-01`

**آخر commit:** `7ed3722c2635eef88540925539edaab06203dc6d`

**المستودع:** [youns03/Midad-app-pro](https://github.com/youns03/Midad-app-pro)

## 1. Current Product State

مِداد هو تطبيق Android أصلي مبني باستخدام Kotlin وJetpack Compose، ويستخدم Room لحفظ المستندات والخطوط المخصصة. يدور المنتج الحالي حول تحرير النص العربي داخل مستند ذي مقاس وهوامش، ثم بناء `DocumentLayoutEngine.DocumentLayout` واحد يُستخدم في المعاينة وتصدير PDF وPNG. يدعم التطبيق القوالب العربية، اختيار الخط، حجم النص، اللون، المحاذاة، الهوامش، مقاس الصفحة، التراجع والإعادة، الكشيدة اليدوية والآلية، الحفظ التلقائي، والمعاينة قبل التصدير.

المنتج يملك أساسًا صحيحًا لتطبيق تنضيد عربي، لكنه لا يزال في مرحلة مبكرة من ناحية إدارة المشاريع الطويلة، الفحص قبل التصدير، الأنماط الطباعية، اتجاه النص المختلط، واستمرارية العمل عبر الأجهزة.

## 2. What Was Inspected

تمت مراجعة بنية المشروع كاملة، بما في ذلك `MainActivity`، الشاشات، `EditorViewModel`، Room entities وDAOs، المستودع، إدارة الخطوط، محرر النص، `KashidaEngine`، `DocumentLayoutEngine`، المعاينة، PDF، PNG، الحوارات، اللوحات السفلية، الثيم، الموارد، الاختبارات، وسير CI.

كما تمت مراجعة دورة العمل كاملة: **الإدخال → المستند → التحرير → التنسيق → التنضيد → المعاينة → التصدير → ملف الناتج**. أُجريت أيضًا مقارنة بحثية مع أدوات التنضيد العربي، محررات المستندات المحمولة، أدوات PDF وتخطيط الصفحات، وتطبيقات الكتابة والتنضيد المحمولة.

## 3. What Was Discovered

### نقاط القوة الحالية

يحافظ التطبيق على النص الخام منفصلًا عن تمثيل العرض. الكشيدة الآلية تُطبّق أثناء التنضيد والرسم ولا تغيّر النص المخزن. كما أن الترحيل إلى صفحات يتم داخل `DocumentLayoutEngine`، ولا يعيد PDF أو PNG إنشاء pagination مستقلة. المعاينة تستخدم صفحات التخطيط نفسها، وتصدير PNG متعدد الصفحات ينشئ صورة لكل صفحة ثم يجمعها في ZIP بدل إنشاء Bitmap عملاقة واحدة.

يدعم الاختبار الحالي حالات عربية مختلطة مع الإنجليزية والأرقام والتشكيل، ويحافظ على الأسطر الفارغة وهوامش النقاط، ويتحقق من عدم تقسيم السطر بين الصفحات. كما أن CI يشغّل اختبارات الوحدة ثم بناء Debug APK.

### المشكلات التقنية المهمة

تستخدم قاعدة البيانات حاليًا `.fallbackToDestructiveMigration()`. هذا يحمي من توقف التطبيق عند تغيير schema، لكنه قد يحذف مستندات المستخدم عند حدوث ترقية مستقبلية غير مغطاة بترحيل صريح. هذه مخاطرة P1 تتطلب تصميم migrations قبل إضافة حقول أو جداول جديدة.

كان عرض الهوامش داخل محرر الورقة يعتمد على العامل التقريبي `0.8`. تم استبداله في Sprint 01 بتحويل نسبي يعتمد على عرض الورقة الفعلي ومقاسها، بحيث يحافظ العرض البصري على النسبة بين المليمتر ومساحة الورقة دون عامل ثابت غير مبرر.

التحرير المباشر يتم عبر `BasicTextField`، بينما التنضيد النهائي يتم عبر `StaticLayout` داخل `DocumentLayoutEngine`. هذا مناسب لفصل التحرير عن العرض، لكنه يعني أن شاشة التحرير ليست معاينة طباعية مطابقة. المعاينة المنفصلة والتصدير هما المرجع الإنتاجي، ويجب أن تبقى هذه الحقيقة واضحة للمستخدم.

لا توجد حاليًا طبقة Preflight تعرض النص الفائض، الخطوط الناقصة، اتجاهات bidi المتعارضة، أو مشكلات قابلية البحث في PDF قبل التصدير. كما لا توجد أنماط فقرات/أحرف أو صفحات رئيسية أو ترقيم صفحات أو فواصل أقسام أو حواشٍ.

### نتائج العربية والتنضيد

المحرك الحالي يثبت مبادئ مهمة: استخدام `TextDirectionHeuristics.RTL`، الحفاظ على النص الخام، إبقاء التشكيل قبل موضع إدخال الكشيدة، وعدم تقسيم السطر بين الصفحات. لكن فرض RTL على كل التخطيط يحتاج إلى توسيع اختبارات bidi للنصوص اللاتينية والروابط والرموز والأقواس، لأن النص المختلط لا يكفي فيه عكس الاتجاه العام وحده.

الكشيدة الحالية محدودة ومقصودة. هي مناسبة كبداية لملء الفراغ داخل السطر، لكنها ليست بديلًا عن محرك shaping عربي كامل يدعم كل خصائص OpenType وعلامات mark-to-mark والخطوط المتغيرة. هذا تحسين معماري لاحق، وليس من الآمن إعادة كتابته ضمن Sprint صغير.

### نتائج الاستيراد

أصبح استيراد TXT وMD متاحًا عبر Android Document Picker. يتحقق المسار من الامتداد الفعلي `.txt` أو `.md`، ولا يعتمد على MIME type وحده. يعامل Markdown كنص خام قابل للتحرير، ويقرأ UTF-8 باستخدام decoder صارم يرفض البيانات غير الصالحة بدل استبدالها بصمت. لا يطلب صلاحيات تخزين قديمة ولا يستخدم مسارات ثابتة.

### نتائج التصدير

يستخدم PDF وPNG وPreview نفس `DocumentLayout` ونفس `drawPage`. PDF يكتب صفحة لكل `PageLayout`. PNG يكتب ملفًا لكل صفحة، ثم يعيد PNG منفردًا في المستند ذي الصفحة الواحدة أو ZIP للمستند متعدد الصفحات. لم يتم إنشاء pagination جديدة في exporters.

لم يتمكن هذا Sprint من فتح ملف PDF أو PNG الناتج بصريًا داخل Sandbox، ولم يتوفر Emulator أو جهاز Android لتشغيل السيناريوهات التفاعلية. لذلك لا يصح وصف فحص المخرجات بأنه `OUTPUT VERIFIED`.

## 4. Priority Matrix

| Priority | Area | Problem/Opportunity | Action | Status |
|---|---|---|---|---|
| P0 | Build and regression safety | يجب ألا تمر تغييرات التنضيد دون اختبار وبناء | CI يشغّل `testDebugUnitTest` ثم `assembleDebug` | Implemented and verified |
| P0 | Raw text/import | استيراد TXT/MD يحتاج تحقق امتداد وUTF-8 آمن | Document Picker، تحقق امتداد، قراءة صارمة، رسائل خطأ | Implemented and tested in CI |
| P0 | Layout consistency | عوامل تقريبية في عرض الهوامش قد تجعل التحرير مضللًا | ربط الهوامش بأبعاد الورقة الفعلية | Implemented and built |
| P0 | Arabic integrity | التشكيل وRTL والمختلط يحتاجون تغطية مستمرة | اختبارات Kashida والتشكيل والنص المختلط والأسطر والصفحات | Partially implemented; expand next |
| P1 | Persistence | destructive migration قد تفقد مستندات المستخدم | تصميم Room migrations قبل تغيير schema | Roadmap |
| P1 | Export trust | لا يوجد Preflight قبل التسليم | فحص النص الفائض والخطوط والاتجاه وقابلية البحث | Roadmap |
| P1 | Long documents | لا توجد أنماط أو صفحات رئيسية أو ترقيم أو فواصل أقسام | تصميم نموذج مستند طويل قابل للتوسع | Roadmap |
| P1 | Typography | لا توجد أنماط فقرات/أحرف أو تحكم OpenType متقدم | بناء typography model تدريجيًا | Roadmap |
| P2 | Interchange | لا يوجد DOCX/RTF/EPUB | إضافة صيغ تبادل بعد تثبيت المصدر والتنسيق | Roadmap |
| P2 | Collaboration | لا يوجد سجل إصدارات أو تعليقات أو مزامنة | تصميم offline-first وسجل نسخ | Roadmap |
| P3 | Smart assistance | مساعدات ذكية وتدقيق عربي متقدم | طبقة اقتراحات قابلة للقبول/الرفض | Deferred |

## 5. Features Actually Implemented

تم تنفيذ استيراد TXT وMD الآمن، واختبارات الحفاظ على النص الخام، والتحقق من UTF-8، ومعالجة الملفات غير المدعومة. تم الحفاظ على Markdown كنص خام دون HTML أو parser أو تحويل تنسيق.

تم استبدال تحويل الهوامش التقريبي بقياس نسبي مبني على عرض الورقة ومقاسها. هذا التغيير محافظ ولا يمس `DocumentLayoutEngine` ولا سلوك Kashida ولا بنية التصدير.

تم الإبقاء على Adaptive Icon والتحسينات السابقة المنقولة إلى Sprint 01، مع عدم تغيير `applicationId`، والحفاظ على `versionCode = 2` في الفرع.

## 6. Features Intentionally Not Implemented

لم تُنفذ DOCX أو RTF أو EPUB أو التعاون أو سجل الإصدارات أو الصفحات الرئيسية أو الحواشي أو الفهارس أو Preflight الشامل. هذه الوظائف تحتاج نماذج بيانات وتصميمًا معماريًا مستقلًا، وتنفيذها سريعًا سيؤدي إلى نسخة سطحية غير موثوقة.

لم يُستبدل shaping الحالي بمحرك خارجي كبير. السبب هو أن ذلك يتطلب قرارًا متعلقًا بحجم المكتبة، الترخيص، Android NDK أو bridge مناسب، وقياسات مقارنة قبل التغيير. تم تثبيت الاختبارات الحالية بدل إخفاء هذا القرار تحت إعادة كتابة غير مختبرة.

## 7. Tests Added and Executed

تتضمن الاختبارات الحالية حالات عربية، إنجليزية، نص مختلط، أرقام، علامات ترقيم، تشكيل، combining marks، أسطر فارغة، نص طويل، صفحات متعددة، عدم تقسيم السطر، الهوامش، الحفاظ على النص الخام، واستيراد TXT/MD وUTF-8.

نفّذ GitHub Actions بنجاح:

```text
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

نتيجة CI: [Build Android APK — Sprint 01](https://github.com/youns03/Midad-app-pro/actions/runs/35619285719)

**BUILD VERIFIED.**

**AUTOMATED TESTING VERIFIED.**

ظهر تحذير KSP داخل سجل GitHub Actions، لكنه لم يفشل المهمة ولم يمنع الاختبارات أو البناء. يجب متابعته في دورة مستقلة إذا تكرر أو أصبح حاجبًا.

## 8. Runtime, Visual, and Output Testing

لم يتوفر Emulator أو جهاز Android في هذه البيئة. لذلك لم يتم تنفيذ فتح التطبيق والكتابة والتحديد والتنسيق والتصدير تفاعليًا.

لم يتم فتح PDF أو PNG الناتج بصريًا في هذه الدورة. جرت مراجعة مسارات الإخراج والـ layout statically، مع تحقق CI من نجاح التجميع والاختبارات.

التصنيف الدقيق هو:

- **STATIC/CODE VERIFIED:** بنية المشروع، مسار الاستيراد، محرك التنضيد، exporters، UI wiring.
- **BUILD VERIFIED:** GitHub Actions بنجاح.
- **AUTOMATED TESTING VERIFIED:** `testDebugUnitTest` بنجاح.
- **RUNTIME TESTING:** غير متاح في Sandbox.
- **VISUAL TESTING:** غير منفذ على Emulator.
- **OUTPUT TESTING:** لم يكتمل فتح وفحص PDF/PNG بصريًا.

## 9. APK and Signing

تم بناء Debug APK ورفعه كـ CI artifact. حافظ المشروع على:

```text
applicationId = com.aistudio.kashidaeditor.artext
versionCode = 2
```

تعذر تنزيل artifact النهائي داخل Sandbox في محاولة الفحص الأخيرة بسبب تعليق `gh run download`، ولذلك لا أدرج بصمة APK أو شهادة جديدة دون تحقق مباشر.

خطوة التوقيع الثابت في workflow كانت متخطاة لأن GitHub Secrets التالية غير متاحة:

```text
DEV_KEYSTORE_BASE64
DEV_KEYSTORE_PASSWORD
DEV_KEY_ALIAS
DEV_KEY_PASSWORD
```

لذلك ما زال التوقيع النهائي الثابت غير مثبت، ولا ينبغي اعتبار تحديث APK فوق نسخة موقعة بمفتاح قديم مضمونًا.

## 10. Recommended Roadmap

### Sprint 02 — Export Trust and Preflight

إضافة نموذج فحص قبل التصدير يحدد النص الفائض، الخطوط غير المتاحة، الصفحات الفارغة غير المقصودة، اتجاهات النص غير المتوقعة، وقابلية البحث الأساسية في PDF. ينبغي أن يكون لكل تحذير اختبار وموضع واضح داخل المعاينة.

### Sprint 03 — Typography Model

إضافة أنماط فقرات وأحرف، line spacing محفوظ في المستند، وإعدادات تنضيد مستقلة عن واجهة التحرير. يجب أن يظل النص الخام مصدر الحقيقة، وأن تُطبق السمات في layout فقط.

### Sprint 04 — Long Documents

إضافة فواصل صفحات وأقسام، ترقيم صفحات، رؤوس وتذييلات اختيارية، وقوالب طويلة. يجب قبل ذلك استبدال destructive migration بترحيلات Room صريحة.

### Sprint 05 — Arabic Bidi and Shaping QA

بناء corpus عربي مختلط يضم الحركات الكثيفة، الأقواس، الروابط، الأرقام العربية والغربية، اللغات ذات الخط العربي، والبحث والنسخ. بعد القياس يمكن دراسة دمج shaping engine ناضج.

### Sprint 06 — Interchange and Collaboration

دراسة DOCX/RTF/EPUB وسجل الإصدارات والعمل دون اتصال قبل إضافة التعاون. يجب تعريف ما يحفظه كل تنسيق وما يفقده، مع اختبارات round-trip حيثما كان ذلك ممكنًا.

## 11. Final Assessment

اقترب Midad في Sprint 01 من خط إنتاج أكثر اتساقًا: استيراد نص موثوق، تخطيط صفحات مركزي، هوامش واجهة محسوبة من أبعاد الورقة، واختبارات CI ناجحة. لكنه ليس بعد تطبيق نشر عربي احترافيًا كاملًا.

أهم قرار منتجي هو عدم توسيع النطاق عشوائيًا. الخطوة التالية ذات أعلى قيمة هي **Export Trust and Preflight**، لأنها تربط جودة التنضيد بما يراه المستخدم في الملف النهائي، وتمنع إعلان نجاح التصدير دون فحص قابلية التسليم.

## References

[1]: https://learn.microsoft.com/en-us/typography/script-development/arabic "Microsoft Typography: Arabic Script Development"

[2]: https://unicode.org/reports/tr9/ "Unicode Standard Annex #9: The Bidirectional Algorithm"

[3]: https://helpx.adobe.com/indesign/desktop/language-and-proofing/arabic-and-hebrew/set-up-arabic-and-hebrew.html "Adobe InDesign: Set up Arabic and Hebrew"

[4]: https://helpx.adobe.com/indesign/desktop/print/preflight/configure-and-use-the-preflight-panel.html "Adobe InDesign: Configure and use Preflight"

[5]: https://helpx.adobe.com/indesign/desktop/print/page-set-up-and-printer-marks/print-bleed-and-slug-areas.html "Adobe InDesign: Bleed and Slug Areas"

[6]: https://github.com/harfbuzz/harfbuzz "HarfBuzz Text Shaping Engine"

[7]: https://support.apple.com/guide/pages-iphone/export-to-word-pdf-or-another-file-format-tance1161f26/ios "Apple Pages: Export to Word, PDF, or another file format"

[8]: https://support.apple.com/guide/pages-iphone/use-bidirectional-text-tan297cf5754/ios "Apple Pages: Use bidirectional text"

[9]: https://www.adobe.com/acrobat/features.html "Adobe Acrobat Features"

[10]: https://wiki.scribus.net/canvas/Scribus_Output "Scribus Output Documentation"

[11]: https://ulysses.app/writeabook "Ulysses: Writing a Book"

[12]: https://ia.net/writer "iA Writer"
