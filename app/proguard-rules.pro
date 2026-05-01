# FinSave release shrinking rules.
# Keep these rules targeted so R8 can still optimize aggressively.

# Room database, entities, DAOs, and generated implementations.
-keep class com.finsave.data.local.FinSaveDatabase { *; }
-keep class com.finsave.data.local.FinSaveDatabase_Impl { *; }
-keep class com.finsave.data.local.entity.** { *; }
-keep @androidx.room.Dao interface com.finsave.data.local.dao.** { *; }
-keep class com.finsave.data.local.dao.**_Impl { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Domain enums persisted or parsed by Room/domain logic.
-keepclassmembers enum com.finsave.domain.model.TransactionType { *; }
-keepclassmembers enum com.finsave.domain.model.AccountType { *; }
-keepclassmembers enum com.finsave.domain.model.BudgetPeriod { *; }
-keepclassmembers enum com.finsave.domain.model.SplitType { *; }
-keepclassmembers enum com.finsave.domain.model.sms.TransactionType { *; }

# Hilt entry points and generated components.
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class *_HiltModules_* { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }
-keepnames class * extends dagger.hilt.internal.GeneratedComponent
-keepnames class * extends dagger.hilt.internal.GeneratedComponentManager

# WorkManager and Hilt workers.
-keep @androidx.hilt.work.HiltWorker class * { *; }
-keep class com.finsave.app.workers.** { *; }
-keep class com.finsave.app.sync.SmsSyncWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# SQLCipher native bridge and support helper.
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class androidx.sqlite.** { *; }

# JSON parsing models and loaders.
-keep class org.json.** { *; }
-keep class com.finsave.domain.model.sms.** { *; }
-keep class com.finsave.data.local.sms.** { *; }

# Kotlin metadata, continuations, and coroutine internals used by suspend APIs.
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }
-keep class kotlin.coroutines.Continuation
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin serialization, if introduced by future models.
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Jetpack Compose keeps most generated code itself; keep preview metadata harmlessly.
-keep class androidx.compose.runtime.saveable.Saver { *; }
-keep @androidx.compose.ui.tooling.preview.Preview class * { *; }
-dontwarn androidx.compose.ui.tooling.**

# Parcelable implementations.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keep class * implements android.os.Parcelable { *; }

# App BuildConfig.
-keep class com.finsave.app.BuildConfig { *; }
