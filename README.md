# ItemForge
Plugin item RPG custom (stat + ability/trigger) untuk Paper. Mirip konsep MMOItems + ExecutableItems, kode 100% orisinal.

## Build
Butuh JDK 21+ dan Maven:  `mvn package`  -> `target/ItemForge-1.0.0.jar` -> taruh di folder plugins/.
Ubah `paper.version` di pom.xml agar sesuai versi Paper servermu.

## Command
/ifg give <player> <id> [jumlah] | list | info <id> | reload   (izin: itemforge.admin)

## Resource pack
- `custom-model-data` dan/atau `item-model` di item YAML harus cocok dengan resource pack.
- Bedrock (Geyser): daftarkan mapping custom item di Geyser (custom_mappings / pack converter) agar model sama muncul.

## ModelEngine
Action `MODEL:<modelId>:<tick>` menempelkan model ModelEngine ke target/pemain (via reflection, aman tanpa ModelEngine).

## Trigger
RIGHT_CLICK, LEFT_CLICK, SHIFT_RIGHT_CLICK, HIT_ENTITY, KILL_ENTITY, WHEN_HIT, CONSUME

## Action
COMMAND, PLAYER_COMMAND, MESSAGE, SOUND, PARTICLE, POTION, HEAL, DAMAGE_NEARBY, IGNITE_NEARBY, LAUNCH, LIGHTNING, TELEPORT_FORWARD, MODEL

## Build lewat GitHub (tanpa install Java/Maven)
1. Buat repo baru di GitHub, upload seluruh isi folder ini (termasuk `.github/`).
2. Buka tab **Actions** -> workflow **Build ItemForge** jalan otomatis tiap push.
3. Setelah hijau, buka run-nya -> bagian **Artifacts** -> download `ItemForge-jar`.
4. Rilis: buat tag `v1.0.0` -> jar otomatis terlampir di halaman **Releases**.
