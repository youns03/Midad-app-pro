# Midad 1.x — Product Blueprint & Deep Product Audit

**المرحلة:** Product Blueprint مستقلة، وليست Sprint تنفيذية.

**الفرع:** `manus/product-blueprint-1x`

**نقطة البداية:** `origin/main` عند commit `de6073d`.

**الحالة:** لا تغييرات تطبيقية في هذه المرحلة. تم إنشاء التقرير فقط بعد تحليل المنتج والكود والمقارنة المرجعية والتحقق البنائي.

## Executive conclusion

Midad اليوم هو **محرر Android محلي للنص العربي مع تخطيط صفحات وتصدير PDF/PNG**. يمتلك أساسًا هندسيًا مهمًا: النص الخام محفوظ في Room، والكشيدة الآلية مشتقة وقت التنضيد، و`DocumentLayoutEngine` هو المسار المركزي الذي تعتمد عليه المعاينة وPDF وPNG.

لكن Midad ليس بعد نظام نشر عربي مكتملًا. وجود محرر نص، أو منتقي خط، أو زر تصدير، أو محرك كشيدة محدود لا يثبت اكتمال المنتج المقابل. الفجوة الأساسية تقع بين ما يراه المستخدم أثناء التحرير وما يحتاج إلى الوثوق به في الملف النهائي.

الرؤية المناسبة لـ Midad 1.x ليست تقليد InDesign أو Word أو Google Docs. الرؤية هي:

> **سير عمل Android عربي موثوق: نص قابل للتحرير → تركيب عربي صحيح → تخطيط صفحات حتمي → معاينة تمثل الناتج → فحص قبل التصدير → ملف نهائي قابل للتسليم.**

لذلك لا أوصي ببدء Sprint 02 تلقائيًا على أنه Preflight فقط. الأولوية الصحيحة هي بناء **عقد جودة للنص والتخطيط والإخراج**، ثم تنفيذ الميزات التي تعتمد عليه بالترتيب.

## 1. ما هو Midad اليوم؟

يبدأ التطبيق بشاشة منزلية محلية تعرض المستندات والقوالب والخطوط والإعدادات. يستطيع المستخدم إنشاء مستند، فتحه، نسخه، حذفه، البحث في عنوانه ومحتواه، وتصدير المستند المفتوح إلى PDF من قائمة المشاركة. يستطيع داخل المحرر تعديل العنوان والنص، اختيار الخط وحجمه ولونه، اختيار لون الصفحة، تغيير المحاذاة، تحديد الهوامش ووحدتها، اختيار A4 أو A5، تفعيل الكشيدة وتحديد مستواها، تطبيق كشيدة يدوية، ثم فتح معاينة متعددة الصفحات وتصدير PDF أو PNG.

يُحفظ المستند محليًا عبر Room. يوجد autosave مؤجل بعد 600ms من التغيير عندما يكون الخيار مفعلاً. توجد تهيئة تلقائية لمستند ترحيبي عند عدم وجود مستندات.

التحرير الحالي يستخدم `BasicTextField`، بينما التخطيط الإنتاجي يستخدم `StaticLayout` داخل `DocumentLayoutEngine`. هذا فصل مقبول، لكنه يعني أن شاشة الكتابة ليست وحدها معيار الوفاء الطباعي. المعاينة والتصدير هما المساران اللذان يجب أن يثبت تطابقهما مع المستخدم.

## 2. رحلة المستخدم الفعلية

| المرحلة | ما يستطيع المستخدم فعله | ما يحدث فعليًا | التوقع الطبيعي | التقييم |
|---|---|---|---|---|
| فتح التطبيق | رؤية المستندات والقوالب والخطوط والإعدادات | Room يحمّل القائمة، وإذا لم توجد وثيقة ينشئ Welcome Document | بدء واضح دون فقدان عمل | يعمل محليًا، لكن لا توجد حالة استرداد أو مزامنة موثقة |
| إنشاء مستند | الضغط على زر إنشاء مستند | ينشئ مستندًا فارغًا بإعدادات افتراضية ثم يفتحه | مستند جديد مستقل قابل للتسمية والحفظ | يعمل، لكن لا يوجد تدفق استيراد نص من الشاشة الرئيسية في `main` |
| فتح مستند | فتح بطاقة أو اختيار فتح من القائمة | يقرأ snapshot ويحمّل الحقول إلى الحالة | عودة كاملة إلى آخر حالة | يعمل، مع خطر fallback صامت للخط إذا أصبح الملف مفقودًا |
| إعادة التسمية | تحرير العنوان في أعلى المحرر | `onTitleChanged` يحفظه عبر autosave | اسم مستند مستقر وقابل للعثور عليه | يعمل، لكن لا يوجد حوار rename أو تأكيد/سجل تغيير |
| الحذف | اختيار حذف من القائمة | يحذف مباشرة من Room | حماية من الحذف العرضي | **UX Problem:** لا يظهر تأكيد أو Undo في المسار المقروء |
| النسخ | اختيار تكرار | ينسخ كل إعدادات الوثيقة ويضيف `(نسخة)` | نسخة مستقلة واضحة | يعمل محليًا |
| استيراد النص | لا يوجد مسار ظاهر في `main` لإنشاء مستند من TXT/MD | الاستيراد الذي أُنجز سابقًا موجود في فرع آخر، وليس نقطة انطلاق هذا Blueprint | إدخال محتوى خارجي آمن مع الحفاظ على Unicode والأسطر | **Missing Feature في main** |
| التحرير | كتابة النص وتغييره والتراجع والإعادة | `BasicTextField` يحفظ النص الخام، وundo/redo يحتفظان بآخر 50 حالة نصية | مؤشّر واختيار وتنسيق دقيقان | النص الأساسي يعمل؛ نموذج rich text غير موجود |
| النص العربي | كتابة العربية والتشكيل والكشيدة | التخطيط يفرض `TextDirectionHeuristics.RTL` ويطبق Kashida وقت التخطيط | shaping وbidi وmarks وpunctuation صحيحة | **Quality Gap:** الاختبارات لا تثبت كل حالات bidi/shaping |
| النص المختلط | إدخال إنجليزية وأرقام داخل العربية | يمر إلى StaticLayout باتجاه RTL عام | ترتيب منطقي للمقاطع والأرقام والأقواس والروابط | **Architectural limitation/verification gap** |
| الخط | اختيار خطوط مدمجة أو استيراد خط من مسار UI | FontManager يحفظ الخط داخل التخزين الخاص ويعيد إنشاء Typeface | معرفة دعم الخط للغة والحركات وإمكانية تضمينه | الاختيار والاستيراد الأساسيان يعملان؛ لا يوجد font preflight |
| الحجم | زيادته أو تقليله أو ضبطه | يُحصر بين 10 و72pt | قياس متوقع في المحرر والتصدير | يعمل، لكن لا توجد styles أو line-height قابل للتحكم |
| المحاذاة | يمين، وسط، يسار، ضبط كامل | تُحوّل إلى `StaticLayout` alignment | محاذاة عربية متسقة مع التسويغ | تعمل جزئيًا؛ التسويغ العربي لا يساوي shaping عربي كامل |
| الهوامش | تغيير قيم مم/سم | تحفظ بالملليمتر، ويحوّل المحرك إلى نقاط | نفس الهوامش في المحرر والمعاينة والتصدير | الحساب المركزي جيد؛ عرض الورقة ليس دليلًا على اختبار بصري |
| إعداد الصفحة | A4 أو A5، لون الصفحة | `PageSize` محدود إلى مقاسين | إعدادات صفحة قابلة للفهم | يعمل ضمن نطاق محدود؛ لا اتجاه landscape أو مقاس مخصص |
| الكشيدة | تشغيل/تعطيل، اختيار مستوى، أو تطبيق يدوي | آلية render-time؛ اليدوية تغيّر raw text | تمديد بصري لا يكسر التشكيل ولا يلوث المصدر آليًا | المبدأ صحيح، لكن التغطية العربية جزئية |
| المعاينة | رؤية كل الصفحات وأزرار PDF وPNG | `PreviewExportDialog` يستهلك نفس layout، ويعرض كل صفحة عبر `drawPage` | المعاينة تمثل الملف النهائي | معماريًا جيد؛ لا يوجد output visual verification |
| الصفحات | معرفة العدد ورؤية الصفحات | `pages.size` هو مصدر العدد، والتقسيم لا يقسم line fragment | حدود صفحات حتمية، overflow ظاهر، ترقيم حقيقي | pagination الأساسية موجودة؛ overflow/preflight غير موجود |
| PDF | تصدير ومشاركة | يرسم كل `PageLayout` داخل `PdfDocument` | PDF قابل للبحث والثبات والتسليم | التوليد موجود؛ جودة الخطوط والبحث لم تُثبت بصريًا |
| PNG | تصدير ومشاركة | صفحة واحدة PNG، وعدة صفحات ZIP من صور منفصلة | ملف واضح الاسم والجودة والصفحات | يعمل معماريًا؛ لم يُفتح الناتج بصريًا في هذه البيئة |
| الملف النهائي | مشاركة Uri عبر FileProvider | generic error message عند الفشل | معرفة نوع الملف ونجاحه وقابليته للفتح | **Quality Gap:** لا preflight ولا تقرير إخراج ولا تحقق فتح |

## 3. خريطة قدرات المنتج الحالية

الحالات في الجدول تعني: **موجود** = كود ومسار واضحان، **جزئي** = يعمل في نطاق محدود أو بلا إثبات كافٍ، **مفقود** = لا يوجد في `main`، **خطر** = موجود لكن يهدد الثقة أو البيانات.

| المجال | القدرة | الحالة | الدليل أو الملاحظة |
|---|---|---|---|
| Documents | إنشاء وفتح وحفظ وحذف ونسخ | موجود | `EditorViewModel.kt` و`DocumentRepository.kt` و`HomeScreen.kt` |
| Documents | إعادة التسمية | موجود جزئيًا | تحرير العنوان موجود؛ لا يوجد مسار مستقل لإعادة التسمية |
| Documents | autosave | موجود جزئيًا | تأخير 600ms؛ لا توجد استعادة جلسة أو تعارضات |
| Documents | الاستيراد | مفقود في baseline main | لا توجد نقطة text picker في `MainActivity.kt` على هذا الفرع |
| Documents | إدارة المستندات | موجود جزئيًا | قائمة، بحث، نسخ، حذف؛ لا مجلدات أو ترتيب يدوي أو مشاريع |
| Documents | persistence | خطر | `.fallbackToDestructiveMigration()` في `AppDatabase.kt` |
| Editing | إدخال raw text | موجود | `BasicTextField` و`EditorUiState.text` |
| Editing | undo/redo | موجود جزئيًا | stack نصي محدود إلى 50؛ لا undo للتنسيق أو العمليات المركبة |
| Editing | selection/cursor semantics | جزئي وغير مثبت | يعتمد على `BasicTextField`؛ لا اختبار تفاعلي فعلي في Emulator |
| Editing | blank lines/newlines | موجود جزئيًا ومختبر | اختبارات `KashidaEngineTest` تغطي الأسطر الفارغة والفواصل |
| Editing | paragraphs/semantic blocks | مفقود | لا نموذج فقرة أو عنوان أو نطاق منسق |
| Typography | خطوط مدمجة | موجود | Amiri/Cairo/Tajawal/Aref Ruqaa/System |
| Typography | استيراد خط | موجود جزئيًا | نسخ داخلي والتحقق بإنشاء Typeface؛ لا تحذير glyph أو embedding |
| Typography | الحجم واللون | موجود | مجال حجم 10–72pt ولوحات اختيار اللون |
| Typography | المحاذاة | موجود جزئيًا | أربع قيم؛ لا نموذج اتجاه فقرة مستقل |
| Typography | RTL/LTR | جزئي | StaticLayout يستخدم RTL دائمًا؛ لا API اتجاه للقصة/الفقرة/المقطع |
| Typography | line spacing | ثابت | `lineSpacingMultiplier = 1.35f` في المحرك ولا خيار محفوظ للمستخدم |
| Typography | paragraph behavior | مفقود | لا keep-with-next أو first-line indent أو widow/orphan policy |
| Typography | Kashida | جزئي | قواعد اتصال يدوية وrender-time shaping محدود |
| Typography | diacritics/combining marks | جزئي | توجد حماية موضعية في Kashida؛ لا shaping corpus شامل |
| Typography | Arabic shaping | جزئي/غير مكتمل | reliance on Android StaticLayout مع Kashida مخصصة؛ لا تحقق World-Ready كامل |
| Page Layout | A4/A5 | موجود | `PageSize` |
| Page Layout | margins and mm→pt | موجود | التحويل داخل `DocumentLayoutEngine` |
| Page Layout | multi-page pagination | موجود جزئيًا | تقسيم lines إلى صفحات؛ لا overflow report |
| Page Layout | page breaks | مفقود | لا فواصل يدوية أو فواصل أقسام |
| Page Layout | page numbering | مفقود | يعرض عدادًا فقط؛ PDF يستخدم page index داخليًا دون footer مرئي |
| Page Layout | headers/footers | مفقود | لا نموذج لها |
| Page Layout | blank pages | جزئي | المستند الفارغ ينتج صفحة فارغة؛ لا policy للصفحات الفارغة المقصودة |
| Page Layout | overflow | مفقود كتحذير | line width قد يتجاوز content width ولا يوجد preflight |
| Preview | صفحة لكل layout | موجود | `PreviewExportDialog` يمرر `documentLayout.pages` |
| Preview | نفس renderer | موجود | `DocumentLayoutEngine.drawPage` مستخدم في preview وPDF وPNG |
| Preview | تطابق بصري مثبت | غير مثبت | لا Emulator ولا فتح output بصري في هذه المرحلة |
| Export | PDF | موجود جزئيًا | `PdfExporter` يرسم layout؛ لا output profile أو preflight |
| Export | PNG متعدد الصفحات | موجود جزئيًا | Bitmap لكل صفحة ثم ZIP؛ لا فحص output بصري |
| Export | file naming | موجود جزئيًا | sanitized title مع timestamp؛ لا naming preset أو metadata |
| Export | quality profiles | مفقود | لا screen/print profiles أو resolution policy للمستخدم |
| Export | font embedding/search | غير مثبت | لا اختبار PDF فعلي أو font inspection في هذه المرحلة |
| UI/UX | Home, Editor, dialogs, sheets | موجود | Compose screens/components/dialogs/sheets |
| UI/UX | loading | موجود جزئيًا | مؤشر تصدير؛ لا حالات loading واضحة لتحميل البيانات الأولية |
| UI/UX | errors | جزئي | Snackbar generic؛ أخطاء font/export لا تعرض تشخيصًا قابلًا للإصلاح |
| UI/UX | discoverability | جزئي | أدوات كثيرة داخل horizontal toolbar/sheets؛ لا مسار import text في baseline |
| UI/UX | accessibility | غير مثبت | توجد content descriptions لبعض الأيقونات؛ لا audit تباين/semantic tree/touch targets |
| QA | static/code | موجود | مراجعة مصدرية واختبارات patterns |
| QA | build | موجود | GitHub Actions ناجح على baseline branch |
| QA | automated | موجود جزئيًا | unit/Robolectric/Kashida؛ لا integration export/output tests |
| QA | runtime/interaction | مفقود | لا Emulator أو جهاز متاح |
| QA | visual/output | مفقود في هذه البيئة | لم تُفتح APK/PDF/PNG بصريًا |

## 4. Output Quality Map

| نقطة التحول | خطر الاختلاف | الدليل الحالي | الإجراء المطلوب |
|---|---|---|---|
| Input → raw text | فقدان Unicode أو الأسطر أو اتجاه النص | `EditorUiState.text` وRoom content | عقد استيراد واختبارات round-trip، وتثبيت TXT/MD في main |
| raw text → StaticLayout | اختلاف bidi أو fallback font أو wrapping | `setTextDirection(TextDirectionHeuristics.RTL)` | corpus عربي مختلط، اتجاه فقرة/مقطع، وقياسات موثقة |
| StaticLayout → LayoutLine | line metrics لا تمثل كل glyph/mark | `paint.measureText` وline bounds | اختبارات combining marks والخطوط الفعلية، لا Typeface.DEFAULT فقط |
| LayoutLine → Kashida | الكشيدة قد لا توافق shaping أو تزيل tatweel موجودًا | `shapeLine` يبدأ بـ`stripKashida(line)` | فصل manual tatweel عن auto kashida، واختبار preservation |
| LayoutLine → pagination | نص طويل بلا spaces قد يتجاوز العرض | `drawPage` يستخدم `maxOf(contentWidth, measured width)` | overflow detection وpolicy واضحة للنص غير القابل للكسر |
| pagination → preview | preview uses same page objects | `PreviewExportDialog` و`drawPage` | إبقاء المسار المشترك وإضافة golden comparison |
| pagination → PDF | Canvas/PDF font and metrics may differ | `PdfExporter` calls `drawPage` | فتح PDF، فحص قابلية البحث والشهادة البصرية |
| pagination → PNG | pixel scaling and ZIP semantics | `ImageExporter` page-per-bitmap | فحص dimensions، sharpness، filenames، ZIP entries |
| font selection → output | missing custom font may silently fallback | `FontManager.getNativeTypeface` returns default on failure | font status/preflight and explicit warning |
| margins → output | invalid values may collapse content width | `coerceAtLeast(1f)` | reject invalid page geometry and report it |
| page count → UI | counter is not navigation or footer numbering | UI shows `page 1 of pageCount` | distinguish page count, current page, and printed page number |

## 5. Arabic typography audit

### Supported now

يحافظ التطبيق على Unicode النصي، ويستخدم Android `StaticLayout` مع اتجاه RTL، ويضم اختبارات للكشيدة والتشكيل والعربية مع الإنجليزية والأرقام، ويمنع الكشيدة الآلية من تغيير النص المخزن. كما أن البحث عن نقاط الاتصال يتجاوز الحركات المرافقة للحرف قبل إدخال tatweel، وهو قرار صحيح لحماية موضع التشكيل في الحالات التي تغطيها القاعدة.

### Partially supported

الكشيدة ليست shaping engine. هي خوارزمية لإدخال tatweel في نقاط اتصال محددة، مع سقف لكل نقطة ومحاولة استهلاك deficit بالقياس. هذا مفيد بصريًا، لكنه لا يثبت صحة الأشكال السياقية، أو mark-to-mark، أو ligatures، أو أنواع الأرقام، أو قواعد bidi المحايدة.

الـ RTL الحالي يحدد اتجاه StaticLayout للكتلة كاملة. هذا لا يساوي تمثيل اتجاه القصة والفقرة والمحرف. النص العربي والإنجليزي والأرقام والأقواس والروابط يحتاج اختبارات على ترتيب المقاطع ونقطة الإدراج والاختيار، لا اختبار احتواء النص فقط.

### Important evidence-based risk

في `KashidaEngine.shapeLine` يتم إنشاء `clean = stripKashida(line)` قبل إعادة بناء النص المرئي. إذا احتوى raw text على tatweel يدوي وكان auto kashida مفعلاً، فهناك احتمال أن يُزال tatweel الموجود أثناء العرض، رغم أن النص المخزن لم يتغير. هذا **Quality Gap وقد يصبح Bug مرئيًا** في النصوص التي تحتوي كشيدة يدوية. يجب اختبار الحالة قبل أي توسعة في الكشيدة.

في `DocumentLayoutEngine.drawPage` يُعاد بناء `StaticLayout` لكل line، ويُستخدم width يساوي الأكبر بين content width وقياس النص. هذا يمنع القص داخل الرسم لكنه قد يسمح لسطر طويل غير قابل للكسر بالخروج بصريًا من منطقة المحتوى بدل إظهار تحذير. هذا **Output Quality Gap** وليس حلًا مكتملًا للـ overflow.

### Not supported or not proven

لم يثبت وجود دعم كامل لـ Arabic shaping عبر كل الخطوط، أو دعم لغة مرتبطة بالفقرات، أو أنواع أرقام قابلة للاختيار، أو عزل محارف LTR، أو علامات اقتباس وأقواس مختلطة، أو روابط ونصوص محايدة، أو إدراج صحيح داخل نص mixed-direction. لا ينبغي وصف Midad بأنه World-Ready أو Arabic publishing engine كامل قبل وجود corpus واختبارات runtime/output.

## 6. Findings classified by problem type

### BUG candidates

**B-01 — احتمال إزالة tatweel اليدوي أثناء auto kashida.** الدليل هو `shapeLine` الذي يبدأ بـ`stripKashida(line)`. السلوك المتوقع هو الحفاظ على النص اليدوي المرئي ما لم يطلب المستخدم إزالته. الأثر هو اختلاف النص المعروض عن نية المستخدم في المستندات التي تحتوي على tatweel يدوي. الأولوية P1 لأن الأثر على الناتج العربي مباشر. الاختبار المطلوب هو raw text يحتوي tatweel مع auto kashida on/off ومقارنة render text.

**B-02 — حذف المستند بلا تأكيد.** `HomeScreen` يربط خيار الحذف مباشرة بـ`viewModel.deleteDocument`. السلوك المتوقع في منتج موثوق هو تأكيد أو Undo، خصوصًا لأن الحذف لا يملك مسار استعادة ظاهرًا. الأولوية P1 من ناحية سلامة بيانات المستخدم، ويجب اختبار التفاعل على runtime.

### UX Problems

**U-01 — واجهة التصدير تقول نجاحًا عامًا دون وصف خصائص الناتج.** `exportMessage` يكتفي برسالة مثل “تم تجهيز PDF بنجاح”، ولا يوضح عدد الصفحات أو مسار الملف أو ما إذا كان PNG متعدد الصفحات ZIP. الأثر هو ضعف الثقة وسوء فهم نوع الملف. الأولوية P1 لأن القرار يؤثر على التسليم.

**U-02 — عداد الصفحة يبدو كترقيم لكنه ليس ترقيمًا مطبوعًا.** المحرر يعرض “صفحة ١ من N”، بينما لا يوجد footer أو page number داخل PDF. يجب تسمية العداد كعدد صفحات أو بناء نظام ترقيم حقيقي. الأولوية P1 من ناحية توقعات المستخدم.

**U-03 — الأدوات موجودة داخل toolbar أفقي وsheets، لكن لا يوجد مسار إدخال نص واضح في baseline main.** هذا يجعل المستخدم يرى إنشاء مستندًا فارغًا وقوالب، لكنه لا يرى استيراد نص خارجي. الأولوية P1 لأن الإدخال هو بداية workflow.

### Missing Features

**M-01 — استيراد TXT/MD إنتاجي في main.** الحل التجريبي السابق لا يمثل baseline هذا الفرع. القيمة عالية، والتعقيد متوسط، والاعتماد هو عقد raw text/metadata واختبارات Unicode.

**M-02 — Preflight.** لا يوجد فحص overflow أو الخطوط أو اتجاهات mixed-direction أو الصفحات الفارغة أو خصائص الإخراج. القيمة عالية جدًا لأن المخرج هو المنتج.

**M-03 — أنماط Paragraph/Character وبنية دلالية.** raw text وحده لا يدعم عناوين أو اقتباسات أو تحديثًا مركزيًا. القيمة عالية للمستندات الطويلة، والتعقيد مرتفع إذا صُمم بلا نطاق.

**M-04 — page breaks، page numbering، headers/footers، section breaks.** هذه غير موجودة. لا ينبغي إضافتها قبل page model قابل للتوسعة.

**M-05 — font compatibility report.** يستطيع المستخدم استيراد Typeface، لكن لا يعرف إن كان الخط يغطي الحركات أو الرموز أو سيظهر fallback في PDF.

### Architectural Limitations

**A-01 — نموذج المستند نص واحد وإعدادات عامة.** `DocumentEntity` يحتوي `content` وسلسلة خصائص عامة، لكنه لا يمثل paragraph spans أو styles أو language runs أو page objects. هذا يمنع التوسع الآمن إلى الأنماط والتعليقات والصفحات الرئيسية.

**A-02 — BasicTextField مقابل final typesetting.** التحرير والـ final layout مساران مختلفان. هذا ليس خطأ بحد ذاته، لكنه يحتاج عقدًا صريحًا يوضح ما يراه المستخدم أثناء الكتابة وما سيظهر في preview، وإلا يصعب تفسير اختلاف المؤشر أو wrapping.

**A-03 — destructive migration.** `fallbackToDestructiveMigration()` يجعل إضافة schema مستقبلية مخاطرة فقدان مستندات. يجب استبداله بترحيلات صريحة قبل توسيع نموذج البيانات.

**A-04 — اتجاه واحد عام للنص.** فرض RTL داخل StaticLayout لا يكفي لبناء bidi model. توسيع الوظيفة يتطلب metadata للفقرة/المقطع أو محركًا ناضجًا بعد إثبات الحاجة.

### Quality Gaps

**Q-01 — عدم اختبار PDF/PNG الفعلي.** نجاح build لا يثبت قابلية فتح الملف أو تطابق glyphs والهوامش والصفحات.

**Q-02 — عدم وجود output profiles.** PDF الحالي واحد، ولا يفرق بين screen وprint، ولا يعلن compression أو font policy أو resolution policy.

**Q-03 — fallback صامت للخط.** `getNativeTypeface` يعود إلى `Typeface.DEFAULT` عند الفشل. يجب ألا يختفي الفرق عن المستخدم في ملف نهائي.

**Q-04 — لا يوجد output manifest.** ZIP متعدد الصفحات لا يملك تقريرًا يذكر ترتيب الصفحات أو عددها أو مصدرها، ورسالة المشاركة عامة.

### Future Ideas

التعاون اللحظي، السحابة، التعليقات، Track Changes، DOCX/EPUB/HTML، Material Sheets، TOC متقدم، دمج البيانات، PDF/X، ICC، press-ready، وتكافؤ InDesign/Scribus أفكار مستقبلية أو مسارات مستقلة. لا ينبغي إدخالها قبل إصلاح الأساس المحلي للنص والإخراج.

## 7. Practical comparison with reference products

| المنتج/الأداة | المبدأ المفيد | ما يحله | الفجوة الحالية في Midad | ما يمكن استخلاصه | التعقيد والقيمة |
|---|---|---|---|---|---|
| Adobe InDesign Arabic | World-Ready Composer، اتجاه القصة/الفقرة/المحرف، shaping، styles، Parent Pages، Preflight | يضمن تركيب النص العربي وإدارة المستند الطويل قبل التسليم | لا توجد bidi model كاملة، styles، Parent Pages أو Preflight | نموذج أصغر لاتجاه النص، styles، صفحات ثابتة وفحص فعلي | عال جدًا؛ قيمة World-Ready وPreflight عالية |
| Scribus | فصل تخطيط الصفحة عن المحتوى، Master Pages، Preflight، مخرجات PDF حسب الغرض | يقلل أخطاء الطباعة والنص الفائض والعناصر خارج الصفحة | لا يوجد overflow check أو page templates أو profiles | Preflight تدريجي ومسارات screen/print بلا ادعاء PDF/X | عال؛ قيمة مباشرة للوثائق متعددة الصفحات |
| Google Docs | مستند دلالي، تعليقات، اقتراحات، إصدارات، تعاون، offline sync | يحل مراجعة النص واستمرارية العمل بين الأشخاص والأجهزة | Room محلي وليس collaboration، ولا styles/versions/comments | بناء model دلالي وتاريخ محلي قبل السحابة | التعاون عال جدًا، ليس أولوية الآن |
| Microsoft Word mobile | Mobile View مقابل Print Layout، autosave، مشاركة بصلاحيات، PDF | يفصل القراءة السريعة عن الوفاء الطباعي ويجعل التسليم واضحًا | لا يوجد فصل view workflow أو مشاركة بصلاحيات | وضع تحرير وإخراج واضحان، مع preflight قبل PDF | متوسط إلى عال؛ قيمة UX/output |
| Apple Pages | bidi صريح، اتجاه الفقرة، تصدير بصيغ ذات أغراض مختلفة | يحل النص المختلط ويمنع افتراض أن كل المستند RTL واحد | لا يوجد اتجاه فقرة/مقطع أو عقد صيغ موثق | اختبارات mixed-direction واتفاقية لكل صيغة | متوسط؛ قيمة عالية للنص العربي المختلط |
| Ulysses | المصدر النصي المنظم، Groups/Sheets، Export Preview، styles وTOC | يقلل إعادة التنسيق ويجعل التصدير قابلًا للتكرار | وثيقة واحدة بلا structure/styles/profiles | Export profile محدود ونموذج headings قبل كثرة الصيغ | عال؛ قيمة لاحقة للمستندات الطويلة |

المبدأ المشترك هو أن المنتج الناضج لا يقيس نجاحه بعدد الأزرار. يقيسه بقدرة المستخدم على الانتقال من مصدر قابل للتحرير إلى ملف نهائي ثابت مع معرفة ما قد يتغير أو يفشل.

## 8. Midad 1.x Product Blueprint

### 8.1 Current Product

محرر عربي محلي سريع، مناسب لكتابة نص قصير أو بدء مستند من قالب، مع خيارات أساسية للخط واللون والحجم والهوامش والمحاذاة والكشيدة وتصدير PDF/PNG.

### 8.2 Current Capabilities

المنتج يملك التخزين المحلي، قائمة مستندات، قوالب عربية، خطوطًا مدمجة، استيراد خطوط، تحرير نص، undo/redo نصي، autosave مؤجل، تخطيط صفحات مركزي، معاينة متعددة الصفحات، PDF، وPNG متعدد الصفحات كـZIP.

### 8.3 Current Limitations

لا توجد في baseline main بنية مستند دلالية، أو استيراد نص ظاهر، أو اتجاه bidi قابل للضبط، أو overflow/preflight، أو styles، أو page breaks، أو page numbering، أو headers/footers، أو output profiles، أو تحقق runtime/output.

### 8.4 Missing Capabilities

الأكثر أهمية هي عقد RTL/shaping، استيراد نص موثوق، نموذج paragraph/style محدود، pagination مع overflow، معاينة إخراج قابلة للتحقق، preflight، حماية persistence عبر Room migrations، وfont compatibility feedback.

### 8.5 Proposed Product Areas

المجالات المقترحة هي: **Arabic Text Integrity**، **Document Model**، **Canonical Layout**، **Page Production**، **Export Trust**، **Local Persistence**، ثم **Project Extensions**. التعاون والسحابة مجال مستقل لاحق.

### 8.6 Feature Map

| المجال | الآن | الهدف 1.x | لا يُبنى الآن |
|---|---|---|---|
| الإدخال | كتابة مباشرة وقوالب | TXT/MD آمن، headings اختيارية، import report | DOCX parser كامل |
| النص العربي | RTL عام وكشيدة محدودة | bidi/shaping contract، corpus واختبارات | محرّك خارجي كبير بلا prototype |
| المستند | وثيقة واحدة بإعدادات عامة | paragraph/style metadata، naming، recovery | collaboration model |
| الصفحات | A4/A5 وpagination | page breaks، overflow، numbering، parent-lite | desktop frame system كامل |
| المعاينة | pages من layout مركزي | output preview مع diagnostics | معاينة منفصلة عن الناتج |
| PDF/PNG | موجودان | profiles محدودة، manifest، checks | PDF/X/ICC بلا إثبات |
| الاستمرارية | Room وautosave | migrations، recovery، export history محلي | cloud sync |
| UX | Home/Editor/sheets | import واضح، delete confirmation، error actions | polish تجميلي واسع |

### 8.7 Workflow Map

المسار المستهدف هو: يفتح المستخدم التطبيق، ينشئ أو يستورد النص، يرى حالة المصدر والملف، يحرر النص مع اتجاه واضح، يختار الخط والحجم والقواعد، يحدد إعداد الصفحة، يفتح Preview الناتج نفسه، يراجع التحذيرات المرتبطة بالصفحة، يختار profile، ثم يحصل على PDF أو PNG/ZIP مع عدد صفحات واسم ونوع واضحين.

يجب أن يكون الرجوع من الناتج إلى المصدر ممكنًا. لا ينبغي تحويل PDF إلى ملف العمل الأساسي. كما يجب أن تكون كل التحويلات المشتقة قابلة لإعادة البناء من raw text والmetadata.

### 8.8 Output Quality Map

جودة الناتج تقاس عبر خمس بوابات: سلامة المصدر، صحة الاتجاه والتشكيل، حتمية pagination، تطابق preview/export، ثم قابلية فتح وتسليم الملف. لا يكفي نجاح Gradle أو وجود URI.

### 8.9 Technical Dependencies

الترتيب التقني الضروري هو: عقد raw text وmetadata، ثم اختبارات bidi/shaping، ثم نموذج paragraph/style محدود، ثم pagination/overflow، ثم page metadata وnumbering، ثم Preflight وExport Preview، ثم profiles. Room migrations يجب أن تسبق توسيع schema.

### 8.10 Product guardrails

يجب الحفاظ على `DocumentLayoutEngine` كمصدر الحقيقة. يجب أن تستخدم Preview وPDF وPNG نفس layout. يجب ألا تعدل Auto Kashida raw text. يجب ألا تستخدم estimated page count أو عوامل تقريبية جديدة. يجب ألا تدمج إلى main من فروع التحليل أو prototype.

## 9. Prioritization model

الأولوية لا تعتمد على الرأي فقط. استخدمت القيمة للمستخدم، تكرار الاستخدام، خطر الفشل، التعقيد، الاعتماد، وتأثير المخرج النهائي.

| Item | Priority | User value | Frequency | Risk | Complexity | Dependency | Output impact | Why now |
|---|---:|---:|---:|---:|---:|---|---:|---|
| تثبيت عقد Arabic/Bidi fixtures | P0 | عال جدًا | كل مستند عربي | عال | متوسط | layout الحالي | عال جدًا | يمنع قرارات مبنية على RTL عام |
| اختبار/إصلاح manual tatweel مع auto | P0 | عال | نصوص عربية منسقة | عال | منخفض | Kashida الحالي | عال | احتمال اختلاف مرئي مباشر |
| overflow contract | P0 | عال جدًا | كل مستند طويل | عال | متوسط | pagination | عال جدًا | يمنع ملفًا يبدو ناجحًا وهو مقصوص |
| استيراد TXT/MD في main | P1 | عال | عند إعادة استخدام نص | متوسط | متوسط | raw text contract | متوسط | بداية workflow خارج النص الفارغ |
| Room migrations/recovery | P1 | عال | كل مستخدم مستمر | عال | متوسط | schema plan | غير مباشر | يحمي البيانات قبل styles |
| font compatibility/preflight | P1 | عال | كل تصدير بخط | عال | متوسط | font manager/layout | عال | يمنع fallback صامتًا |
| paragraph/character styles محدودة | P1 | عال | مستندات طويلة | متوسط | عال | document model | عال | أساس الاتساق والقوالب |
| page breaks/numbering/headers | P1 | متوسط إلى عال | مستندات متعددة الصفحات | متوسط | عال | page model/styles | عال | يحول pages إلى إنتاج فعلي |
| Export Preview + output manifest | P1 | عال جدًا | كل تسليم | عال | متوسط | layout/preflight | عال جدًا | يجعل الناتج قابلًا للثقة |
| DOCX/EPUB/HTML | P2 | متوسط | حسب الجمهور | عال | عال جدًا | semantic model | متوسط | لا يُبنى قبل PDF/PNG الموثوق |
| collaboration/cloud | P3 | قد يكون عال | غير معلوم | عال جدًا | عال جدًا | backend/identity | غير مباشر | خارج جوهر المنتج المحلي الآن |

## 10. Roadmap after Blueprint

### Sprint 02 — Arabic Integrity Contract

**الهدف:** تحويل صحة النص العربي من افتراض إلى عقد قابل للاختبار.

**المشكلة:** RTL العام والكشيدة المحدودة لا يثبتان shaping أو mixed-direction أو preservation للكشيدة اليدوية.

**النطاق:** corpus عربي عادي ومشكل، Arabic+English، Arabic+numbers، punctuation، parentheses، quotes، neutral characters، blank lines، spaces، وtatweel اليدوي. إضافة tests للـ raw text، rendered text، line bounds، واتجاهات المقاطع. فحص الحاجة إلى shaping prototype قبل إدخال مكتبة.

**المكونات:** `KashidaEngine.kt`، `DocumentLayoutEngine`، اختبارات Kashida، وربما طبقة model جديدة للاتجاه.

**الاعتماد والمخاطر:** لا يدمج محركًا خارجيًا قبل قياس الفشل. نجاحه يتطلب مقارنة runtime أو golden output، لا unit tests فقط.

**التحقق والنجاح:** كل fixture يحافظ على النص المصدر، لا يضع tatweel في موضع خاطئ، لا يكسر سطرًا، ويعطي ترتيبًا متوقعًا في Preview/PDF/PNG عند توفر output testing.

### Sprint 03 — Document Model and Safe Ingestion

**الهدف:** جعل المستند قابلًا لإعادة الاستخدام دون إتلاف raw text.

**المشكلة:** `DocumentEntity.content` وإعدادات عامة لا تمثل paragraphs، headings، language runs، styles، أو استيراد خارجي ثابت.

**النطاق:** Room migrations صريحة، استيراد TXT/MD إلى main، تمثيل paragraph metadata محدود، وعقد حفظ/استعادة. لا DOCX parser.

**المكونات:** `Entities.kt`، `AppDatabase.kt`، `Daos.kt`، `DocumentRepository.kt`، `MainActivity.kt`، import codec، والاختبارات.

**التحقق والنجاح:** migration لا تفقد وثيقة، import يحافظ على Unicode/newlines، reopen يعيد نفس المصدر، وexport بعد reopen يطابق export قبل الإغلاق.

### Sprint 04 — Deterministic Page Production

**الهدف:** جعل pagination وoverflow وpage metadata منتجًا واضحًا.

**المشكلة:** pagination موجودة، لكن page breaks، overflow، numbering، headers/footers غير موجودة، والسطر غير القابل للكسر قد يتجاوز المحتوى بلا تحذير.

**النطاق:** overflow diagnostics، manual page breaks، page metadata، current page distinction، numbering policy، وربما header/footer بسيط مصدره page model. لا Parent Pages كاملة بعد.

**المكونات:** `DocumentLayoutEngine`، document model، Editor UI، Preview، PDF exporter، tests.

**التحقق والنجاح:** كل overflow يظهر كتحذير مرتبط بالصفحة، page count يساوي `pages.size`، لا line split، وترقيم المعاينة وPDF متسق.

### Sprint 05 — Styles and Reusable Templates

**الهدف:** تقليل التنسيق اليدوي في الوثائق الطويلة.

**المشكلة:** لا توجد Paragraph/Character Styles، ولذلك لا يوجد تعديل مركزي للعناوين والمتن والاقتباسات.

**النطاق:** styles محدودة قابلة للتوريث، عنوان/متن/اقتباس/قائمة، تطبيق على paragraph ranges، وحفظها. لا selectors معقدة أو CSS clone.

**التحقق والنجاح:** تغيير style ينعكس على كل النطاقات، لا يغير raw text، ويطابق Preview/PDF/PNG.

### Sprint 06 — Export Trust and Preflight

**الهدف:** جعل قرار التصدير قابلًا للثقة.

**المشكلة:** وجود PDF/PNG ونجاح build لا يثبت فتح الملف أو مطابقته للمعاينة.

**النطاق:** Preflight فعلي لـoverflow، missing font/glyph، invalid page geometry، blank page policy، mixed-direction warnings، وعدد الصفحات. Export Preview يستهلك output layout نفسه. Output manifest يذكر النوع والصفحات والاسم.

**التحقق والنجاح:** اختبارات تفتح PDF/PNG، تقارن dimensions وعدد الصفحات والنص المتوقع، وتثبت أن التحذيرات تمنع الادعاء بنجاح غير مستحق.

### Sprint 07 — Parent-lite and Output Profiles

**الهدف:** إعادة استخدام عناصر الصفحات وتوضيح غرض الملف.

**المشكلة:** headers/footers/page numbers والقوالب المتكررة غير موجودة، ولا توجد screen/print profiles.

**النطاق:** parent-lite محدود، profile للشاشة وprofile للطباعة المكتبية، مع توثيق دقيق للخطوط والدقة والضغط. لا PDF/X أو ICC أو press-ready.

### Sprint 08 — Project Structure and Interchange Evaluation

**الهدف:** تقييم الوثائق الطويلة والصيغ الإضافية بعد ثبات الأساس.

**المشكلة:** Groups/chapters وDOCX/EPUB/HTML غير موجودة.

**النطاق:** prototype صغير فقط مع قرار مبني على حالات الاستخدام. لا يبدأ إلا إذا نجحت مخرجات PDF/PNG والأنماط وPreflight.

### Deferred — Collaboration and Cloud

لا يُقترح Sprint تعاون حاليًا. يحتاج ذلك إلى قرار منتج، identity، backend، sync queue، conflict policy، version history، security، واختبارات منفصلة. وجود Room أو autosave المحلي ليس dependency كافية.

## 11. Verification plan

| مستوى التحقق | ما تم في هذه المرحلة | ما لم يتم |
|---|---|---|
| Static / Code | قراءة المصدر، جرد الملفات، تحليل المسارات، patterns، مراجعة invariants | لا يوجد lint شامل جديد |
| Build | GitHub Actions baseline على `manus/product-blueprint-1x` نجح | لا build محلي بسبب غياب Android SDK المحلي |
| Automated | CI شغّل `:app:testDebugUnitTest` ونجح | لا اختبارات جديدة لأن هذه المرحلة تحليلية |
| Runtime | غير منفذ | لا Emulator أو جهاز Android في البيئة |
| Interaction | غير منفذ | لا كتابة/تحديد/حذف/استيراد/تصدير تفاعلي فعلي |
| Visual | غير منفذ | لا screenshot audit أو معاينة جهاز |
| Output | مسارات PDF/PNG راجعتها source-level | لم يُفتح PDF/PNG بصريًا ولم تُقارن ملفات فعلية |
| Regression | baseline build/tests ناجحة | لا دليل على regression بصري أو مخرجات فعلية |

**Build result:** PASS على [CI run 35648360693](https://github.com/youns03/Midad-app-pro/actions/runs/35648360693).

**Product result:** NOT YET VERIFIED، لأن runtime وvisual وoutput لم تُنفذ في هذه البيئة. لا تساوي نتيجة build نتيجة المنتج.

## 12. What must not be built now

لا ينبغي الآن بناء تعاون سحابي، أو محرر Word كامل، أو تكافؤ InDesign/Scribus، أو كل صيغ DOCX/EPUB/HTML، أو PDF/X/ICC/press-ready، أو نظام أنماط معقد، أو UI polish واسع. هذه العناصر قد تكون ذات قيمة مستقبلية، لكنها تعتمد على صحة النص ونموذج المستند والتخطيط والتحقق.

كذلك لا ينبغي إعلان أن import TXT/MD أو Preflight أو Styles أو Parent Pages موجودة في `main` لمجرد وجود prototype سابق أو زر واجهة. وجود feature في UI لا يساوي اكتمال workflow.

## 13. Final answers to the success criteria

**ما هو Midad اليوم؟** محرر عربي محلي مع تخطيط صفحات أساسي وتصدير PDF/PNG، وليس بعد منصة نشر عربي كاملة.

**ما الذي ينقصه فعليًا؟** عقد bidi/shaping، ingestion ثابت في main، نموذج مستند دلالي، overflow/page production، styles، font diagnostics، Preflight، ومطابقة output مثبتة.

**ما الذي يمنع نضجه؟** احتمال اختلاف النص العربي المختلط، غياب output verification، destructive migration، fallback صامت للخط، وعدم وجود حماية من overflow أو الحذف العرضي.

**ما الوظائف التي تستحق البناء؟** وظائف تزيد الثقة في الناتج: Arabic integrity، model دلالي محدود، pagination diagnostics، styles محدودة، Preflight، وExport Preview حقيقي.

**ما ترتيب بنائها؟** Arabic contract ثم safe ingestion/model ثم deterministic pages ثم styles ثم Preflight/Export Trust ثم parent-lite/profiles ثم extensions.

**ما الذي لا يبنى الآن؟** التعاون، السحابة، كل صيغ التبادل، PDF/X/ICC، وتكافؤ أدوات النشر المكتبي.

**ما الذي يجب إصلاحه قبل ميزات جديدة؟** tatweel preservation، overflow contract، delete confirmation، Room migrations، font fallback visibility، وتجربة output حقيقية.

**كيف نقيس التحسن؟** بمصفوفة fixtures عربية، ونجاح reopen/round-trip، تطابق page count وbounds، مقارنة Preview/PDF/PNG، معدل overflow غير المكتشف، وضوح رسائل الخطأ، ونتائج runtime/visual/output منفصلة عن build.

## References

[1]: https://helpx.adobe.com/indesign/desktop/language-and-proofing/arabic-and-hebrew/set-up-arabic-and-hebrew.html "Adobe InDesign: Set up Arabic and Hebrew"

[2]: https://helpx.adobe.com/indesign/desktop/language-and-proofing/language-settings/adobe-world-ready-composer-overview.html "Adobe InDesign: World-Ready Composer overview"

[3]: https://helpx.adobe.com/indesign/desktop/language-and-proofing/arabic-and-hebrew/justify-arabic-text.html "Adobe InDesign: Justify Arabic text"

[4]: https://helpx.adobe.com/indesign/desktop/create-and-organize-pages/create-and-manage-parent-pages/about-parent-pages.html "Adobe InDesign: Parent Pages"

[5]: https://helpx.adobe.com/indesign/desktop/print/preflight/configure-and-use-the-preflight-panel.html "Adobe InDesign: Preflight"

[6]: https://wiki.scribus.net/canvas/Help:Manual_PDFworkflow "Scribus: PDF workflow"

[7]: https://wiki.scribus.net/canvas/Help:Manual_PDFx3 "Scribus: PDF/X-3 workflow"

[8]: https://wiki.scribus.net/canvas/Preflight-Verifier-Help-Text "Scribus: Preflight Verifier"

[9]: https://wiki.scribus.net/canvas/Working_with_Master_Pages "Scribus: Working with Master Pages"

[10]: https://workspace.google.com/products/docs/ "Google Docs product overview"

[11]: https://support.google.com/docs/answer/2494822?hl=en&co=GENIE.Platform%3DDesktop "Google Docs: Share files and collaborate"

[12]: https://support.google.com/docs/answer/6388102?hl=en&co=GENIE.Platform%3DDesktop "Google Docs: Version history"

[13]: https://support.microsoft.com/en-us/word/mobile-view-and-print-layout-in-word-mobile "Microsoft Word mobile: Mobile View and Print Layout"

[14]: https://support.microsoft.com/en-us/office/saving-files-in-office-on-android-phones "Microsoft Office: Saving files on Android phones"

[15]: https://support.microsoft.com/en-au/word/save-a-pdf-of-your-file-on-your-mobile-device "Microsoft Word: Save a PDF on a mobile device"

[16]: https://support.apple.com/guide/pages-iphone/use-bidirectional-text-tan297cf5754/ios "Apple Pages: Use bidirectional text"

[17]: https://support.apple.com/guide/pages-iphone/export-to-word-pdf-or-another-file-format-tance1161f26/ios "Apple Pages: Export to Word, PDF, or another file format"

[18]: https://ulysses.app/ "Ulysses writing app"

[19]: https://help.ulysses.app/en_US/export-publishing/export "Ulysses: Export and publishing"

[20]: https://help.ulysses.app/en_US/styles-themes/customize-an-export-style "Ulysses: Customize an export style"
