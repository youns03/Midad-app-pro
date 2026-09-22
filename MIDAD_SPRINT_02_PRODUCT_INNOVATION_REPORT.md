# Midad — Sprint 02 Product Innovation, Deep Audit & Output Benchmark

**الفرع:** `manus/product-innovation-sprint-02`  
**نقطة البداية:** `origin/main` عند `cbe8f6814a251912b9b99b31e2896eab1fc4fd9f`  
**الالتزام الحالي:** `a60320278fd526becbf6614413b0478ae2a6ad4a`  
**الهدف:** بناء فهم منتجي وتقني قابل للتنفيذ لتطوير Midad كمحرر ونظام إخراج عربي، مع تنفيذ مجموعة صغيرة عالية القيمة ومنخفضة المخاطر فقط.

> **حالة هذه المرحلة:** تم تنفيذ التغيير البرمجي المحدود وتشغيل الاختبارات والبناء عبر GitHub Actions بنجاح. لم يتم الادعاء بإجراء تحقق بصري على جهاز Android أو فحص بنيوي كامل لملفات PDF/PNG؛ لذلك يميز التقرير بين النتائج المثبتة والاختبارات التي أصبحت جاهزة للمرحلة التالية.

---

## 1. الملخص التنفيذي

Midad يملك أساسًا صحيحًا لمنتج عربي قابل لإعادة التدفق: النص الخام محفوظ منطقيًا، وواجهة التحرير مبنية على Kotlin/Compose، ومحرك `DocumentLayoutEngine` مركزي يستهلكه Preview وPDF وPNG، مع دعم A4/A5 وKashida وقت الرسم. هذه نقطة قوة حقيقية، لأنها تمنع اختلافات مبكرة بين مسارات العرض والتصدير.

لكن المنتج لم يصل بعد إلى مستوى ناشر عربي مكتمل. الفجوات الجوهرية ليست في إضافة أزرار شكلية، بل في غياب عقد تخطيط قابل للفحص، وغياب styles، وغياب preflight، وغياب page-break semantics، وعدم وجود طبقة واضحة لتسجيل font fallback وخصائص BiDi وclusters وقرارات الكسر. لذلك أوصى Sprint 02 بتثبيت هذه الطبقات تدريجيًا بدل إعادة كتابة `KashidaEngine` أو القفز مباشرة إلى page-layout حر.

تم تنفيذ ثلاثة تغييرات صغيرة: إضافة `Export Readiness` مشتقة من نفس `DocumentLayout`، الحفاظ على الكشيدة اليدوية عند تشغيل Auto Kashida، وإضافة Output Benchmark fixtures واختبارات آلية للحالات العربية والمختلطة. لا يغيّر ذلك نموذج المستند أو قاعدة البيانات أو applicationId أو مسار التصدير.

---

## 2. سؤال المنتج

السؤال المنتجـي ليس: «كيف نضيف مزيدًا من التنسيق؟» بل: **كيف يجعل Midad إخراج النص العربي قابلًا للثقة، وقابلًا لإعادة الإنتاج، وقابلًا للتشخيص قبل أن يشارك المستخدم PDF أو PNG؟**

الإجابة المقترحة هي بناء منتج على ثلاث طبقات مترابطة: محرر يحتفظ بالنص المنطقي، ومحرك تخطيط يملك القياس والكسر والتقسيم الصفحي، ومركز جودة يشرح للمستخدم ما إذا كانت النتيجة آمنة للتصدير وما المشكلات التي ما زالت تحتاج قرارًا تحريريًا.

---

## 3. نطاق Sprint 02

شمل النطاق التدقيق العميق في المسار من إدخال النص حتى التصدير، مقارنة التغييرات منذ Sprint 01، البحث في Unicode وAndroid والنشر العربي والمنتجات المرجعية وإنتاج PDF، بناء corpus Output Benchmark، اختيار مجموعة صغيرة للتنفيذ، وتشغيل CI والبناء.

لم يشمل النطاق إعادة تصميم المشروع، أو قاعدة بيانات جديدة، أو page-layout حر، أو محرك shaping عربي جديد، أو PDF/X وICC كاملين، أو دمج التغييرات إلى `main`.

---

## 4. منهجية التدقيق

اُستخدمت قراءة مباشرة للمصدر الحالي، ومقارنة Git بين فرع Sprint 01 و`origin/main`، وتشغيل baseline CI، ثم بحث متعدد المصادر الرسمية والأكاديمية والمنتجية. فُصلت الملاحظات إلى: مشكلة مثبتة في الكود، فجوة منتجية، فكرة تصميمية، نتيجة benchmark، أو اقتراح مؤجل يحتاج قرارًا أوسع.

المصادر الخارجية لم تُستخدم لإثبات أن Midad يملك ميزة لا يملكها. استخدمت لاستخراج مبادئ قابلة للتحويل إلى خصائص أو اختبارات، مع تسجيل حدود كل مصدر.

---

## 5. ما تغيّر فعليًا منذ Sprint 01

المقارنة تمت بين `origin/manus/product-audit-sprint-01` عند `6c710fa` و`origin/main` عند `cbe8f68`. الفرق الفعلي صغير ومحدد.

| التغيير | الحالة في main الحالي | الأثر |
|---|---|---|
| إضافة محرك `com.example.layout` مستقل | أُضيف في `b1d8f3a` ثم حُذف كملفات زائدة في `cbe8f68` | لم يبقَ مسار layout ثانٍ |
| `DocumentUnits.kt` | باقٍ في `com.example.layout` | تحويلات mm/pt/inch قابلة للاختبار |
| `MainCanvasComponent.kt` | تحسن | الهوامش المرئية أصبحت مرتبطة بنسبة أبعاد الورقة الفعلية |
| `ExampleUnitTest.kt` | تحسن | اختبارات دقة الوحدات أضيفت |
| استيراد TXT/MD | غير موجود في main الحالي | لا تغيير منذ Sprint 01 |
| styles | غير موجودة | فجوة مستمرة |
| preflight | غير موجود قبل Sprint 02 | أضيفت الآن طبقة أولية في فرع Sprint 02 فقط |
| page breaks | غير موجودة | فجوة مستمرة |

أهم استنتاج هو أن حذف المحرك الزائد في `cbe8f68` كان تصحيحًا معماريًا جيدًا: لم يبقَ في المشروع محركا تخطيط متنافسان. يجب الحفاظ على هذا القرار.

---

## 6. baseline الحالي قبل التنفيذ

شُغّل workflow `build-apk.yml` على `main` الحالي في التشغيل `35704776234`. نجحت خطوة `:app:testDebugUnitTest` ونجحت خطوة `:app:assembleDebug`، ونتج APK Debug صالح للرفع كـartifact.

هذا يثبت أن نقطة البداية قابلة للبناء والاختبار. لا يثبت وحده جودة PDF أو PNG المرئية، ولا يثبت تطابق Compose مع rasterization على جهاز فعلي.

---

## 7. خريطة المستخدم الحالية

يبدأ المستخدم من Home، حيث يمكنه فتح مستند أو إنشاءه وإدارته، ثم ينتقل إلى Editor المبني على `BasicTextField` وأدوات التنسيق. ينتقل النص بعد ذلك إلى `EditorViewModel`، الذي يبني `DocumentLayout`، ثم تظهر المعاينة وتُستدعى مسارات PDF وPNG من نفس التخطيط.

هذه الرحلة مناسبة لمنتج word-processing عربي أولًا. لكنها لا تمثل بعد قصة نشر كاملة؛ لا توجد طبقة فقرات/أنماط دلالية، ولا علامات page break، ولا تقارير قبل التصدير، ولا سجل تفصيلي يشرح لماذا انتهى سطر أو صفحة في موضع معين.

---

## 8. خريطة المعمارية الحالية

المعمارية الفعلية الحالية هي: `Room raw text → EditorViewModel → DocumentLayoutEngine → Preview/PDF/PNG`. يستخدم المحرك `StaticLayout` لقياس الأسطر، ثم يحولها إلى صفحات، ويطبق Auto Kashida في مرحلة الرسم/التخطيط دون تعديل النص المخزن.

هذا الاتجاه صحيح. المطلوب في المرحلة القادمة هو تعميق العقد داخل المحرك، لا إنشاء backend آخر لكل مخرج. ينبغي أن يعيد المحرك مستقبلاً `LayoutSnapshot` يتضمن ranges منطقية، runs واتجاهات، clusters، break map، metrics، page assignment وdiagnostics.

---

## 9. مصدر الحقيقة النصي

النص المخزن في Room يجب أن يبقى logical Unicode. لا ينبغي حفظ ترتيب glyphs المرئي، ولا إدخال U+0640 الناتج من Auto Kashida، ولا قلب المقاطع اللاتينية أو الأرقام إلى visual order.

تؤكد [Unicode UAX #9][uax9] أن BiDi يعالج الفقرة ويحل مستويات الاتجاه قبل ترتيب العرض، وتؤكد [HarfBuzz][harfbuzz] أن shaping ينتج glyphs وadvances وoffsets وclusters لا string مرئية بديلة. لذلك يجب أن تبقى نتائج العرض مشتقة وقابلة لإعادة البناء.

---

## 10. BiDi واتجاه النص المختلط

المحاذاة إلى اليمين لا تكفي لتعريف فقرة عربية صحيحة. فقرة RTL قد تحتوي English وSKU وURL وأرقامًا وعلامات ترقيم، ويجب أن يظل ترتيبها المنطقي محفوظًا مع عرض runs بالاتجاه المناسب.

ينبغي أن يصبح `baseDirection` خاصية صريحة على مستوى المستند أو الفقرة، مع وضع تلقائي وoverride يدوي. كما يجب أن يحتفظ layout بنتائج الاتجاه لكي يستطيع preflight تفسير خطأ في قوس أو رقم بدل وصفه بعبارة عامة مثل «النص غير مرتب».

---

## 11. التشكيل والـclusters

العربية ليست مجموعة قواعد أول/وسط/آخر بسيطة. الخط قد يستخدم GSUB/GPOS وligatures ومواضع حركات وبدائل سياقية. لذلك يجب اعتبار cluster، الذي يضم الأساس وعلاماته والعناصر المرتبطة به، وحدة تحرير وكسر.

وفق [UAX #14][uax14] لا تنشئ combining marks نقطة كسر مستقلة. النتيجة العملية لـMidad هي منع break بين الحرف والعلامة، وعدم تقسيم النص إلى chunks أو صفحات قبل اكتمال shaping في السياق الصحيح.

---

## 12. تقييم Kashida الحالي

المبدأ الحالي جيد: النص الأصلي لا يتغير، وAuto Kashida تعمل أثناء layout/render، والقياس يعتمد على المساحة، ولا يفترض المحرك وجود كَشيدة في كل موضع.

المشكلة التي عولجت في Sprint 02 كانت أن `shapeLine` كان يزيل الكشيدة الموجودة في السطر كجزء من intermediate processing. أصبح السلوك الآن محافظًا: إذا احتوى السطر على manual tatweel، تُحفظ الكشيدة اليدوية بدل إسقاطها عند تشغيل Auto Kashida.

هذا ليس إعادة كتابة للمحرك، ولا يدعي أن كل الخطوط أو الأساليب تقبل الكشيدة. ما زالت هناك حاجة إلى `KashidaPolicy` صريحة تستبعد URLs والأرقام والأسماء والمقاطع الحساسة وتملك fallback إلى ragged alignment.

---

## 13. كسر السطر والتقسيم الصفحي

المحرك الحالي يفصل الأسطر ثم يوزعها على الصفحات دون تقسيم السطر بين صفحتين. هذه نقطة سلامة مهمة، ووجود `pageCount` كمشتق من `pages.size` يمنع آلية estimatedPages.

لكن المنتج لا يملك بعد `BreakMap` يشرح سبب كل كسر: hard newline، نهاية فقرة، عرض، page height، أو break rule. كما لا توجد قواعد keep-with-next أو widow/orphan أو manual page break. هذه ليست أخطاء ينبغي ترقيعها داخل PNG؛ إنها جزء من نموذج الصفحة القادم.

---

## 14. الهوامش والوحدات

منذ Sprint 01 إلى main الحالي، تحسن `MainCanvasComponent` باستخدام تحويلات `DocumentUnits` ونسبة scale مرتبطة بعرض الورقة الفعلي. هذا أفضل من عامل تقريبي ثابت لأنه يحافظ على تناسب الهوامش عند اختلاف عرض المعاينة.

يجب الاستمرار في الفصل بين وحدات المستند بالنقاط/المليمتر وبين dp/sp في واجهة Compose. لا ينبغي أن يعيد PDF أو PNG تفسير الهوامش من أبعاد الشاشة.

---

## 15. Preview/PDF/PNG

المسار الحالي يستخدم `DocumentLayoutEngine.drawPage` في Preview وPDF وPNG. وتتعامل ImageExporter مع الصفحات على حدة، ثم تُنتج PNG واحدة للصفحة الواحدة أو ZIP للوثائق متعددة الصفحات، بدل إنشاء Bitmap عملاقة واحدة.

هذه البنية تحقق المبدأ الصحيح: لا pagination مستقلة في exporters. ما ينقص هو توثيق output contract قابل للفحص، وفحص PDF بعد التوليد، ومقارنة line boxes/page map بين المخرجات.

---

## 16. القراءة البحثية: المبادئ الحاكمة

تجمع [W3C Arabic & Persian Layout Requirements][alreq] و[UAX #9][uax9] و[UAX #14][uax14] و[HarfBuzz][harfbuzz] على مجموعة مبادئ: النص المنطقي مصدر الحقيقة؛ BiDi خاصية بنيوية؛ shaping سياقي؛ combining marks لا تنفصل؛ line breaking لا يغير المصدر؛ والتبرير ليس مساواة حسابية عمياء لطول السطر.

هذه المبادئ تجعل Midad مناسبًا للتطوير التدريجي، لكنها تجعل بعض التحسينات السريعة غير مناسبة: إعادة ترتيب النص للمعاينة، إدخال tatweel في Room، تقسيم النص قبل shaping، أو اعتبار right alignment دليلاً على صحة RTL.

---

## 17. القراءة البحثية: Android وStaticLayout

توثق [StaticLayout][staticlayout] أنه تخطيط لنص ثابت بعد layout، مع خيارات width وalignment وspacing وdirection وbreak strategy وhyphenation. لكنه ليس محررًا، ولا نموذج مستند، ولا نظام styles أو preflight.

يمكن أن يظل StaticLayout backend عمليًا داخل Midad، بشرط تغليفه خلف `DocumentLayoutEngine`، وتسجيل خياراته ونتائجه، وعدم افتراض أن قياس Compose يطابقه تلقائيًا في PDF/PNG. يجب تثبيت font وAPI وdensity وprofile في benchmarks.

---

## 18. القراءة البحثية: النشر العربي

توضح مقالة Titus Nemeth في [Journal of Electronic Publishing][nemeth] أن التبرير العربي يختلف عن التبرير اللاتيني؛ وأن الكشيدة ليست تمديدًا هندسيًا عامًا. وتقدم إرشادات Nihad Nadam [تصورًا مهنيًا للنشر العربي][nadam] يشمل اتجاه التجليد، ترتيب الصفحات، الهوامش الداخلية والخارجية، الفصول، الرؤوس، الأرقام، الحواشي والفهارس.

النتيجة المنتجية هي أن A4/A5 وحدهما لا يصنعان ناشرًا عربيًا. يجب أن يصبح لدى Midad مستقبلًا `PageTemplate` و`binding` و`inner/outer margins` و`section`، لكن ليس من الضروري تنفيذها كلها في Sprint 02.

---

## 19. القراءة البحثية: InDesign وScribus وAffinity

يوثق Adobe World-Ready Composer تشكيلًا سياقيًا وBiDi وligatures والتبرير العربي ودرجات Kashida، ويقدم مرجعًا مفيدًا للمبادئ لا وعدًا بتكافؤ Midad مع InDesign. وتشير وثائق وقضية Scribus إلى أن RTL page binding وميزات العربية مرتبطة بالإصدار والتاريخ، لذلك لا يكفي قبول Unicode لإثبات جودة عربية كاملة.

لم تقدم صفحة Affinity المتاحة مادة قابلة للتحقق تثبت كل قدرات RTL/shaping المطلوبة؛ لذلك عوملت Affinity كفجوة تحقق لا كحكم إيجابي أو سلبي. لا ينبغي بناء roadmap على feature parity غير موثق.

---

## 20. القراءة البحثية: Word وGoogle Docs وPages

تظهر المراجع الرسمية أن المنتجات الناضجة تميز بين اتجاه الفقرة ولغة الواجهة، وتعرض أدوات RTL سياقيًا، وتفصل styles الفقرية عن character styles، وتميز word-processing عن page-layout، وتتعامل مع line break وpage break كعلامات بنيوية.

هذا يضع فرصة Midad بوضوح: منتج عربي-first صغير يركز أولًا على word-processing وإخراج A4/A5 موثوق، ثم يبني styles وpage semantics قبل محاولة أن يصبح لوحة نشر حرة.

---

## 21. القراءة البحثية: PDF وPreflight

توضح [Adobe Preflight][preflight] و[Adobe Font Handling][font-handling] و[GWG PDF/X][gwg] أن إنتاج PDF قابل للطباعة يتطلب فحص الخطوط والتضمين، page boxes، الألوان، الدقة، output intent، وإجراءات workflow كاملة. تضمين خط أو subset مشروط بالترخيص ولا يثبت وحده صحة shaping.

ينبغي أن يميز Midad مستقبلاً بين `Screen PNG` و`Print PDF`. يمكن البدء بفحص بنية PDF وعدد الصفحات والخطوط والـMediaBox، لكن لا ينبغي تسمية الناتج PDF/X أو print-ready من دون مكتبة وسير عمل واختبار مطبعي مناسب.

---

## 22. Output Benchmark: سياسة القياس

Output Benchmark في هذه المرحلة يركز على invariants قابلة للقياس: حفظ raw text، عدد صفحات حقيقي، عدم تقسيم السطر، عدم وجود newline داخل line fragment، أبعاد موجبة، وعدم تجاوز عرض المحتوى عندما يكون القياس صالحًا.

تُضاف لاحقًا مقارنات visual وPDF structural. لا يُعامل pixel identity عبر كل الأجهزة كمعيار مطلق، لأن rasterizer وhinting وfont fallback قد تختلف. المعيار الأقوى هو logical correctness وgeometry وglyph coverage وpage map، مع tolerances موثقة.

---

## 23. Output Benchmark: corpus المقترح

| الرمز | الحالة | المؤشرات الأساسية |
|---|---|---|
| A | فقرة عربية قصيرة | اتجاه، قياس، صفحة واحدة |
| B | فقرة عربية طويلة | line breaks، page count، عدم overflow |
| C | عربي مع English | BiDi runs وترتيب المقاطع |
| D | عربي مع أرقام | digits، punctuation، اتجاه الأرقام |
| E | علامات ترقيم واقتباس | الأقواس والنقطتين وعلامات الاقتباس |
| F | tashkeel وcombining marks | cluster integrity وعدم انفصال العلامات |
| G | اقتباس مختلط | فصل logical/visual order |
| H | عناوين وفقرات | hard newlines وparagraph boundaries |
| I | فقرات متعددة | blank lines وحفظ البنية |
| J | فقرة طويلة بلا فواصل اصطناعية | overflow وpagination |
| K | عدة أسطر فارغة | blank-line preservation |
| L | manual Tatweel | عدم فقد محتوى المستخدم |
| M | Auto Kashida | bounded render-time shaping |
| N | مستند متعدد الصفحات | page map وعدم تقسيم السطر |
| O | افتتاح فصل | عنوان، أسطر فارغة، تدفق متعدد الصفحات |

هذه الحالات أضيفت إلى `OutputBenchmarkTest.kt` كاختبارات canonical layout. هي تختبر نواتج المحرك المشتقة، لا تدعي أنها بديل عن مراجعة صور PDF/PNG على جهاز فعلي.

---

## 24. Output Benchmark: البيانات التي يجب تسجيلها

لكل fixture ينبغي تسجيل hash للنص المنطقي، font file/hash، Android API، نسخة المحرك، density، locale، base direction، width بالنقاط، A4/A5، line spacing، justification، kashida policy، break strategy، line count، page count، page geometry، fallback، وdiagnostics.

عند إضافة backend مستقل، ينبغي تسجيل glyph IDs وadvances وoffsets وcluster ranges وlogical-to-visual mapping. ويجب فصل اختلاف الرستر عن اختلاف التشكيل أو الكسر.

---

## 25. Output Benchmark: الحالة المثبتة الآن

تم تشغيل corpus الآلي داخل CI عبر `:app:testDebugUnitTest`، ونجحت الاختبارات. كما نجح `:app:assembleDebug` في نفس التشغيل. هذا يثبت أن fixtures قابلة للبناء والتنفيذ ضمن Robolectric/Android test environment.

لم يتم بعد تشغيل التطبيق على emulator أو جهاز فعلي لاستخراج PNG/PDF ثم مقارنة pixels أو PDF internals. لذلك حالة visual output هي **غير مثبتة بعد** وليست PASS.

---

## 26. Feature Discovery Matrix

| الفرصة | القيمة للمستخدم | المخاطر | ملاءمة المعمارية الحالية | القرار |
|---|---:|---:|---:|---|
| Export Readiness | عالية | منخفضة | ممتازة | نُفذت |
| Output Benchmark | عالية | منخفضة | ممتازة | نُفذ جزئيًا آليًا |
| Arabic Text Preflight الموسع | عالية | متوسطة | جيدة | المرحلة التالية |
| Paragraph/Character Styles | عالية جدًا | عالية | تحتاج تصميمًا | تصميم أولًا |
| Page Break/keep rules | عالية | عالية | تحتاج page model | تصميم أولًا |
| DirectionContext صريح | عالية | متوسطة | يحتاج metadata | تصميم ثم تنفيذ |
| Font manifest/fallback audit | عالية | متوسطة | جيدة | مرحلة تالية |
| Header/footer/folio | متوسطة | متوسطة | تحتاج page semantics | لاحق |
| Facing pages/RTL binding | عالية للنشر | عالية | غير موجودة | مؤجل |
| Page-layout حر | متوسطة | عالية جدًا | غير مناسب الآن | مؤجل |
| PDF/X/ICC | عالية للطباعة التجارية | عالية جدًا | غير متاح حاليًا | مؤجل |

---

## 27. Product Opportunity Matrix

| الفرصة | احتياج المستخدم | الأثر الاستراتيجي | متطلبات ما قبل التنفيذ |
|---|---|---|---|
| محرر عربي موثوق | منع اختلاف المعاينة والتصدير | يبني الثقة الأساسية | LayoutSnapshot وpreflight |
| أنماط عربية قابلة لإعادة الاستخدام | اتساق كتاب/بحث/مقال | يحول Midad من محرر إلى أداة إنتاج | نموذج ParagraphStyle/CharacterStyle |
| إخراج نشر عربي | صفحات وهوامش وفواصل مفهومة | يميز المنتج عن محررات النص العامة | PageTemplate وbreak rules |
| فحص قبل المشاركة | اكتشاف overflow والـfallback | يقلل فشل المستخدم بعد التصدير | Diagnostics مرتبطة بالموقع |
| corpus عربي مفتوح | حماية من regressions | يجعل التطوير قابلًا للقياس | تراخيص ومراجعة fixtures |
| وضع Print-safe | معرفة الفرق بين screen وprint | يزيد قيمة PDF | OutputProfile وPDF verification |

---

## 28. ما نُفذ في Sprint 02

أضيفت `DocumentLayoutEngine.ExportReadiness` مع `ReadinessIssue`. الفحص يكتشف غياب الصفحات، هندسة الصفحة غير الصالحة، النص الخارج من حدود المحتوى، صفحة فارغة داخل مستند متعدد الصفحات، وعدم تطابق page count مع `pages.size`.

أضيفت بطاقة `Export Readiness` إلى نافذة Preview. البطاقة مشتقة من `documentLayout` نفسه، وتعرض الحالة والرسائل العربية، ولا تنشئ pagination أو renderer بديلًا. أزرار التصدير لم تُحذف ولم تُحظر آليًا؛ الفحص يشرح الحالة ولا يتخذ قرارًا تحريريًا قسريًا.

أُصلح حفظ manual tatweel عند تشغيل Auto Kashida، وأضيف اختبار صريح له. أضيفت أيضًا اختبارات readiness الصحيحة والسلبية، و`OutputBenchmarkTest` للحالات A–O.

---

## 29. لماذا هذه التغييرات منخفضة المخاطر

لا توجد migration لقاعدة البيانات، ولا تغيير في `applicationId` أو SDK أو AGP، ولا تعديل في `BasicTextField` architecture، ولا backend تصدير جديد. `ExportReadiness` pure function فوق نموذج قائم، ويمكن حذف بطاقة UI دون المساس بالنص أو التصدير.

إصلاح manual tatweel يحافظ على إدخال المستخدم ولا يضيف U+0640 إلى Room. والاختبارات لا تغير behavior الإنتاجي إلا في حالة كانت الكشيدة اليدوية موجودة أصلًا في line fragment، حيث كان إسقاطها غير مرغوب.

---

## 30. التحقق البرمجي

| الفحص | النتيجة |
|---|---|
| `git diff --check` | PASS |
| baseline `:app:testDebugUnitTest` على main | PASS |
| baseline `:app:assembleDebug` على main | PASS |
| Sprint 02 `:app:testDebugUnitTest` | PASS عبر CI |
| Sprint 02 `:app:assembleDebug` | PASS عبر CI |
| OutputBenchmark fixtures | PASS ضمن unit tests |
| APK artifact | أُنتج بنجاح |
| runtime على جهاز Android | لم يُنفذ |
| visual PDF/PNG comparison | لم يُنفذ |
| PDF structural preflight كامل | لم يُنفذ |

تشغيل Sprint 02 هو workflow `35705537884`، ونجح job البناء كاملًا. ظهر annotation متعلقًا بـKSP Application service، لكنه لم يفشل job ولم يمنع الاختبارات أو البناء. كما ظهرت تحذيرات مستقبلية عن Node/Ubuntu runner، وهي ليست سبب فشل.

---

## 31. APK artifact

تم تنزيل artifact `midad-debug-apk` من تشغيل Sprint 02، ووجد الملف `app-debug.apk` بحجم يقارب 23 MB. قيمة SHA-256 المحلية هي:

`86fbe0bd7ad3834f4c9157f945160ebb4b682298f3c70be022c75e3ab9f90207`

وجود artifact وبناؤه لا يثبت أن النسخة قابلة للتحديث فوق كل APK سابق؛ ذلك يعتمد على ثبات توقيع CI، وهو موضوع تدقيق سابق منفصل ويجب أن يظل ضمن سياسة توقيع التطوير الثابتة.

---

## 32. ما لم نفعله عمدًا

لم نضف styles أو page breaks أو headers/footers، لأن كل واحدة منها تحتاج توسيعًا لنموذج المستند لا زرًا منفردًا. لم نعد كتابة KashidaEngine، ولم نحاول استبدال Android shaping يدويًا.

لم نضف PDF/X أو ICC أو print-ready claim. ولم نغير قاعدة البيانات أو ندمج الفرع في `main`. لم نطلب من المستخدم حذف التطبيق أو تغيير applicationId أو تعديل مفاتيح التوقيع.

---

## 33. ما يحتاج تصميمًا قبل التنفيذ

يحتاج Midad إلى `DocumentStyle` versioned يضم font/fallback وlanguage وbaseDirection وline policy وjustification وKashida policy. ويحتاج إلى نموذج blocks/paragraphs/spans يحافظ على توافق النصوص القديمة.

يحتاج أيضًا إلى `PageTemplate` يفصل trim/content frame وinner/outer margins وbinding، ثم `PageBreakRule` يدعم manual/automatic وkeep-with-next وavoid-orphans. هذه الطبقات يجب أن تدخل إلى `DocumentLayoutEngine` نفسه وتظهر في Preview وPDF وPNG معًا.

---

## 34. ما ينبغي تأجيله

يؤجل محرك shaping عربي يدوي، لأن GSUB/GPOS وclusters أوسع من قواعد أول/وسط/آخر. يؤجل التكافؤ الكامل مع InDesign، والناشر الحر للبطاقات والأغلفة، وPDF/X/ICC التجاري، والتحويل إلى outlines، والتبرير العدواني.

التأجيل ليس رفضًا للفرص، بل حماية لمصدر الحقيقة الحالي. إذا بنيت styles وpage semantics وpreflight أولًا، يصبح تنفيذ هذه الفرص لاحقًا أكثر أمانًا وقابلية للاختبار.

---

## 35. Roadmap المقترحة

| المرحلة | الهدف | المخرجات |
|---|---|---|
| Sprint 03 | عقد التخطيط والتشخيص | `LayoutSnapshot`، `BreakMap`، font/environment metadata |
| Sprint 04 | نموذج styles أولي | ParagraphStyle، CharacterStyle، legacy importer |
| Sprint 05 | page semantics | manual break، keep-with-next، section، header/footer design |
| Sprint 06 | Arabic preflight موسع | clusters، controls، fallback، overflow، diagnostics بالموقع |
| Sprint 07 | PDF/output verification | page boxes، fonts، metadata، layout-to-output comparison |
| Sprint 08 | RTL publishing | binding، facing pages، inner/outer margins، folios |
| لاحق | page-layout وPDF/X/ICC | بعد إثبات word-processing العربي ووجود متطلبات طباعة واضحة |

كل Sprint لاحق يجب أن يبدأ بـbaseline، ويملك corpus صغيرًا، ويضيف اختبارًا سلبيًا، ويشغل CI، ولا يخلط أكثر من محور معماري كبير في دفعة واحدة.

---

## 36. مخاطر وحدود التقرير

المراجع الخارجية تشرح مبادئ وأدوات، ولا تثبت تطابقًا بصريًا بين Midad ومنتج آخر. W3C ALReq متطلبات/مسودة، ومراجع Scribus تاريخية مرتبطة بالإصدار، وصفحة Affinity لم توفر دليلًا قابلًا للاستخراج على كل قدرات العربية.

قد تختلف نتائج StaticLayout وfont fallback وICU/HarfBuzz والرسترينغ باختلاف API والخط والكثافة. لذلك لا ينبغي تحويل line count أو pixel hash إلى حقيقة عامة من دون تثبيت البيئة.

تضمين الخط مشروط بالترخيص. ووجود embedded/subset font لا يثبت صحة shaping أو جودة المظهر. ونجاح unit tests لا يثبت أن PDF مطابق للمعاينة على جهاز المستخدم.

---

## 37. القرار النهائي والتوصية

**قرار Sprint 02:** PASS للتنفيذ المحدود والتحقق الآلي، مع بقاء التحقق البصري وPDF structural verification كفجوة معلنة، لا كمشكلة مخفية.

**التوصية:** احتفظ بفرع `manus/product-innovation-sprint-02` كفرع تطوير مستقل، وراجع commit `a603202` قبل أي دمج. لا تبدأ Batch جديدًا داخل هذا النطاق. المرحلة التالية المنطقية هي تصميم `LayoutSnapshot + PreflightReport + StyleSpec` على الورق والكود الاختباري قبل تنفيذ page breaks أو styles واسعة.

**ما الذي يجب ألا يحدث الآن؟** لا تعِد كتابة `KashidaEngine`، لا تضف محرك layout ثانٍ، لا تعدل `main` مباشرة، لا تدعي أن APK print-ready، ولا توسع المنتج إلى page-layout حر قبل تثبيت word-processing العربي.

---

## المراجع

[uax9]: https://www.unicode.org/reports/tr9/ "Unicode Bidirectional Algorithm"
[uax14]: https://www.unicode.org/reports/tr14/ "Unicode Line Breaking Algorithm"
[alreq]: https://www.w3.org/TR/alreq/ "W3C Arabic and Persian Layout Requirements"
[harfbuzz]: https://harfbuzz.github.io/ "HarfBuzz Manual"
[staticlayout]: https://developer.android.com/reference/android/text/StaticLayout "Android StaticLayout"
[nemeth]: https://doi.org/10.3998/3336451.0023.104 "Arabic justification research"
[nadam]: https://nihad.me/book-design-for-arabic-language-publications/ "Arabic book design guidance"
[preflight]: https://helpx.adobe.com/acrobat/using/analyzing-documents-preflight-tool-acrobat.html "Adobe Acrobat Preflight"
[font-handling]: https://helpx.adobe.com/acrobat/desktop/create-documents/explore-advanced-conversion-settings/font-handling-distiller.html "Adobe font handling"
[gwg]: https://gwg.org/pdf-x-workflow/ "Ghent Workgroup PDF/X workflow"

## ملفات الأدلة الداخلية

- مقارنة التغييرات منذ Sprint 01: `sprint02_change_review.md`
- ملخص البحث الخارجي: `sprint02_research_findings.md`
- اختبارات Output Benchmark: `app/src/test/java/com/example/kashida/OutputBenchmarkTest.kt`
- تنفيذ Export Readiness: `app/src/main/java/com/example/kashida/KashidaEngine.kt` و`app/src/main/java/com/example/ui/dialogs/Dialogs.kt`
