package forestry.datagen

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

final class ForestryResourceGenerator {
    private final File projectRoot
    private final List<String> owners = ['main', 'butterflies', 'farms', 'mail']
    private final File contentFile

    ForestryResourceGenerator(Path root) {
        projectRoot = root.toFile().canonicalFile
        contentFile = file('gradle/fabric-f3-content.json')
    }

    static void main(String[] arguments) {
        if (arguments.length != 2) throw new IllegalArgumentException('Expected project root and output directory')
        new ForestryResourceGenerator(Path.of(arguments[0])).generate(Path.of(arguments[1]))
    }

    void generate(Path destination) {
        File outputRoot = destination.toFile().canonicalFile
        Path boundary = new File(projectRoot, 'build/fabric-26.2').toPath()
        if (!outputRoot.toPath().startsWith(boundary) || outputRoot.toPath() == boundary)
            throw new IllegalArgumentException('Resource output must be inside build/fabric-26.2')
        if (outputRoot.exists()) {
            Files.walk(outputRoot.toPath()).withCloseable { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
            }
        }
        def parser = new JsonSlurper()
        def scope = parser.parse(contentFile)
        def roots = owners.collectEntries { owner -> [(owner): [file("src/${owner}/resources"),
            file(owner == 'main' ? 'src/generated/resources' : "src/generated/resources_${owner}")]] }
        def report = [:]
        def writeJson = { File target, Object json ->
            target.parentFile.mkdirs()
            target.setText(JsonOutput.prettyPrint(JsonOutput.toJson(json)) + '\n', 'UTF-8')
        }
        owners.each { owner ->
            def target = new File(outputRoot, owner)
            def allowedItems = (scope.main.items + scope[owner].items).collect { "forestry:${it}".toString() } as Set
            def allowedBlocks = (scope.main.blocks + scope[owner].blocks).collect { "forestry:${it}".toString() } as Set
            def copied = new LinkedHashSet<String>()
            def lookup = { String path ->
                def searchRoots = roots[owner].reverse() + (owner == 'main' ? [] : roots.main.reverse())
                def found = searchRoots.collect { new File(it, path) }.find { it.isFile() }
                // The same sapling artwork is stitched into both native item and block atlases.
                if (found == null && path.startsWith('assets/forestry/textures/block/saplings/'))
                    found = searchRoots.collect { new File(it, path.replace('/textures/block/saplings/', '/textures/item/')) }.find { it.isFile() }
                found
            }
            def copyResource
            def validateModelTextures = { String model ->
                def textures = [:]
                def chain = []
                def seen = new HashSet<String>()
                while (model?.startsWith('forestry:')) {
                    if (!seen.add(model)) throw new IllegalStateException("Cyclic model inheritance: ${model}")
                    def input = lookup('assets/forestry/models/' + model.substring(9) + '.json')
                    if (input == null) throw new IllegalStateException("Missing model: ${model}")
                    def definition = parser.parse(input)
                    chain.add(definition)
                    model = definition.parent
                }
                chain.reverseEach { textures.putAll(it.textures ?: [:]) }
                textures.values().findAll { it instanceof String && it.startsWith('forestry:') }.each { texture ->
                    copyResource('assets/forestry/textures/' + texture.substring(9) + '.png')
                }
            }
            // Composite block geometry becomes native multipart blockstates and composite item models.
            // Follow inherited item models too; keep each original base/overlay model and its rotations.
            def compositeChildren
            compositeChildren = { String model, Set seen = new HashSet() ->
                if (model == null || !model.startsWith('forestry:') || !seen.add(model)) return null
                def source = lookup('assets/forestry/models/' + model.substring(9) + '.json')
                if (source == null) return null
                def definition = parser.parse(source)
                if (definition.loader == 'neoforge:composite') {
                    if (!(definition.children instanceof Map) || !definition.children.values().every { it instanceof String })
                        throw new IllegalStateException("Unsupported composite geometry: ${model}")
                    return definition.children.values().toList()
                }
                definition.parent instanceof String ? compositeChildren(definition.parent, seen) : null
            }
            copyResource = { String path ->
                if (!copied.add(path)) return
                File input = lookup(path)
                if (input == null) throw new IllegalStateException("Missing F3 ${owner} resource: ${path}")
                File output = new File(target, path)
                output.parentFile.mkdirs()
                output.bytes = input.bytes
                if (path.endsWith('.png')) {
                    File animation = lookup(path + '.mcmeta')
                    if (animation != null) new File(target, path + '.mcmeta').bytes = animation.bytes
                }
                if (path.endsWith('.json') && path.startsWith('assets/')) {
                    def json = parser.parse(input)
                    if (path.startsWith('assets/forestry/blockstates/') && json.variants instanceof Map &&
                            json.variants.values().any { it instanceof Map && compositeChildren(it.model) != null }) {
                        def parts = []
                        json.variants.each { state, variant ->
                            if (!(variant instanceof Map)) throw new IllegalStateException("Weighted composite requires explicit conversion: ${path}")
                            def condition = state ? state.split(',').collectEntries { pair -> def fields = pair.split('='); [(fields[0]): fields[1]] } : [:]
                            (compositeChildren(variant.model) ?: [variant.model]).each { child ->
                                def part = [apply: new LinkedHashMap(variant) + [model: child]]
                                if (!condition.isEmpty()) part.when = condition
                                parts.add(part)
                            }
                        }
                        json = [multipart: parts]
                        writeJson(output, json)
                    }
                    if (path.startsWith('assets/forestry/items/') && json.model?.type == 'minecraft:model') {
                        def children = compositeChildren(json.model.model)
                        if (children != null) {
                            json.model = [type: 'minecraft:composite', models: children.collect { child -> new LinkedHashMap(json.model) + [model: child] }]
                            writeJson(output, json)
                        }
                    }
                    if (JsonOutput.toJson(json).contains('neoforge:')) throw new IllegalStateException("Fabric model still uses NeoForge: ${path}")
                    def follow
                    follow = { Object node ->
                        if (node instanceof Map) {
                            node.each { key, value ->
                                if (key in ['model', 'parent', 'base'] && value instanceof String && value.startsWith('forestry:')) {
                                    copyResource('assets/forestry/models/' + value.substring(9) + '.json')
                                    if (key != 'parent') validateModelTextures(value)
                                } else if (key == 'textures' && value instanceof Map) {
                                    value.values().findAll { it instanceof String && it.startsWith('forestry:') }.each { texture ->
                                        def texturePath = 'assets/forestry/textures/' + texture.substring(9) + '.png'
                                        if (lookup(texturePath) != null) copyResource(texturePath)
                                    }
                                } else follow(value)
                            }
                        } else if (node instanceof List) node.each { follow(it) }
                    }
                    follow(json)
                }
            }
            scope[owner].items.each { id ->
                if (id == 'leaves' || id.endsWith('_leaves') || id.endsWith('_default_leaves_fruit')) {
                    writeJson(new File(target, "assets/forestry/items/${id}.json"), [model: [type: 'forestry:leaves']])
                    return
                }
                if (id.startsWith('bucket_')) {
                    writeJson(new File(target, "assets/forestry/items/${id}.json"), [model: [type: 'forestry:fluid_container', fluid: "forestry:${id.substring(7)}",
                        cover_is_mask: false, bucket_mask: true, flip_gas: true, textures: [base: 'minecraft:item/bucket', fluid: 'minecraft:item/bucket']]])
                    return
                }
                def path = "assets/forestry/items/${id}.json"
                copyResource(path)
            }
            scope[owner].blocks.each { id ->
                if (id.startsWith('fluid_')) {
                    def fluid = id.substring(6)
                    copyResource("assets/forestry/textures/block/liquid/${fluid}_still.png")
                    def flow = "assets/forestry/textures/block/liquid/${fluid}_flow.png"
                    if (lookup(flow) != null) copyResource(flow)
                    writeJson(new File(target, "assets/forestry/models/block/${id}.json"), [textures: [particle: "forestry:block/liquid/${fluid}_still"]])
                    writeJson(new File(target, "assets/forestry/blockstates/${id}.json"), [variants: ['': [model: "forestry:block/${id}"]]])
                    return
                }
                if (id == 'sapling_ge') writeJson(new File(target, "assets/forestry/blockstates/${id}.json"), [variants: ['': [model: 'minecraft:block/oak_sapling']]])
                else copyResource("assets/forestry/blockstates/${id}.json")
                def lootId = id.replace('_wall_hanging_sign', '_hanging_sign').replace('_wall_sign', '_sign').replace('_wall_torch', '_torch')
                copyResource("data/forestry/loot_table/blocks/${lootId}.json")
            }

            // Preserve the original merged locale policy: translations live in core.
            if (owner == 'main') {
                copyResource('config/forestry/hints.properties')
                copyResource('config/forestry/colour.properties')
                copyResource('assets/forestry/atlases/gui.json')
                copyResource('assets/minecraft/atlases/gui.json')
                def lootRules = new TreeMap()
                roots.main.each { root -> fileTree(new File(root, 'data/forestry/loot_modifiers/chests')).matching { include '*.json' }.files.each { input ->
                    def rule = parser.parse(input)
                    if (rule.type != 'forestry:condition_modifier') throw new IllegalStateException("Unsupported loot injection: ${input}")
                    lootRules[rule.table] = rule.extensions.collect { extension -> 'forestry:' + rule.table.split(':')[1] + '/' + extension }
                } }
                writeJson(new File(target, 'forestry-loot-injections.json'), lootRules)
                writeJson(new File(target, 'data/forestry/tags/fluid/entity_movement.json'), [values:
                    ['ethanol', 'biomass', 'liquid_glass', 'honey', 'crushed_ice', 'fruit_juice', 'seed_oil', 'short_mead', 'wax', 'experience']
                        .collectMany { ['forestry:' + it, 'forestry:' + it + '_flowing'] }])
                roots.main.each { root -> fileTree(root).matching {
                    include 'data/forestry/worldgen/**', 'data/forestry/structure/**', 'data/forestry/loot_table/chests/**', 'data/forestry/loot_table/gameplay/**', 'data/forestry/trade_set/**', 'data/forestry/villager_trade/**', 'data/**/tags/villager_trade/**', 'data/**/tags/worldgen/**', 'data/**/tags/point_of_interest_type/**', 'data/forestry/advancement/**'
                }.files.each { input ->
                    def path = root.toPath().relativize(input.toPath()).toString().replace('\\', '/')
                    copyResource(path)
                    if (path ==~ /data\/forestry\/worldgen\/configured_feature\/ore_(apatite|tin)\.json/) {
                        def ore = parser.parse(input)
                        ore.type = 'forestry:' + input.name.replace('.json', '')
                        writeJson(new File(target, path), ore)
                    }
                } }
                def locales = [:].withDefault { new TreeMap() }
                roots.values().flatten().each { root ->
                    fileTree(new File(root, 'assets/forestry/lang')).matching { include '*.json' }.files.sort().each { language ->
                        locales[language.name].putAll(parser.parse(language))
                    }
                }
                ['en_us.json': ['config.jade.plugin_forestry.status': 'Forestry status', 'jade.forestry.bee_progress': 'Bee progress: %s%%', 'jade.forestry.ripeness': 'Fruit ripeness: %s%%'],
                 'zh_cn.json': ['config.jade.plugin_forestry.status': '林业状态', 'jade.forestry.bee_progress': '蜂群进度：%s%%', 'jade.forestry.ripeness': '果实成熟度：%s%%']].each { name, entries -> locales[name].putAll(entries) }
                locales.each { name, json -> writeJson(new File(target, "assets/forestry/lang/${name}"), json) }
                new File(target, 'forestry-f3-content.json').bytes = contentFile.bytes
            }

            if (owner == 'main') {
                ['spectacles', 'apiarist_armor'].each { equipment ->
                    copyResource("assets/forestry/equipment/${equipment}.json")
                    copyResource("assets/forestry/textures/entity/equipment/humanoid/${equipment}.png")
                }
                copyResource('assets/forestry/textures/entity/equipment/humanoid_leggings/apiarist_armor.png')
                copyResource('assets/forestry/models/item/filled_crate.json')
                roots.main.each { root -> fileTree(root).matching {
                    include 'assets/forestry/particles/*.json', 'assets/forestry/textures/particle/**'
                }.files.each { input -> copyResource(root.toPath().relativize(input.toPath()).toString().replace('\\', '/')) } }
                writeJson(new File(target, 'assets/minecraft/atlases/blocks.json'), [sources: [
                    [type: 'minecraft:directory', source: 'entity/signs', prefix: 'entity/signs/']
                ]])
                roots.main.each { root -> fileTree(root).matching { include 'assets/forestry/textures/block/engine_*.png', 'assets/forestry/textures/block/rainmaker_*.png', 'assets/forestry/textures/block/*chest.png' }.files.each { input ->
                    copyResource(root.toPath().relativize(input.toPath()).toString().replace('\\', '/'))
                } }
                scope.main.items.findAll { it.endsWith('_default_leaves') }.each { item ->
                    def species = item.substring(0, item.length() - '_default_leaves'.length())
                    if (!(species in ['oak', 'birch', 'spruce', 'jungle', 'acacia', 'dark_oak', 'cherry'])) {
                        copyResource("assets/forestry/models/item/${species}_sapling.json")
                        copyResource("assets/forestry/models/block/${species}_sapling.json")
                    }
                }
                ['drone', 'princess', 'queen', 'larvae'].each { stage ->
                    copyResource("assets/forestry/models/item/bee_${stage}_default.json")
                    if (stage != 'larvae') copyResource("assets/forestry/models/item/bee_${stage}_cube.json")
                }
            }
            roots[owner].each { root -> fileTree(root).matching {
                include 'assets/forestry/textures/gui/**', 'assets/forestry/textures/forestry/atlas/gui/**', 'assets/forestry/textures/entity/boat/**', 'assets/forestry/textures/entity/chest_boat/**', 'assets/forestry/textures/entity/villager/**', 'assets/forestry/textures/entity/zombie_villager/**', 'assets/forestry/textures/block/leaves/**'
            }.files.each { input -> copyResource(root.toPath().relativize(input.toPath()).toString().replace('\\', '/')) } }
            if (owner == 'mail') {
                copyResource('assets/forestry/textures/gui/letter.png')
                copyResource('assets/forestry/textures/gui/mail_toast.png')
                def blocked = new File(target, 'assets/forestry/textures/gui/sprites/container/slots/blocked.png')
                blocked.parentFile.mkdirs()
                blocked.bytes = lookup('assets/forestry/textures/forestry/atlas/gui/slots/blocked.png').bytes
            }
            if (owner == 'butterflies') {
                copyResource('assets/forestry/models/item/butterfly_base.json')
                (roots[owner] + roots.main).each { root -> fileTree(root).matching { include 'assets/forestry/textures/item/butterfly/*.png', 'assets/forestry/textures/entity/butterfly/*.png' }.files.each { input ->
                    copyResource(root.toPath().relativize(input.toPath()).toString().replace('\\', '/'))
                } }
                roots.main.each { root -> fileTree(root).matching { include 'data/forestry/taxon/*.json' }.files.each { input ->
                    def json = parser.parse(input)
                    if (json.type == 'forestry:butterfly_species') writeJson(new File(target, root.toPath().relativize(input.toPath()).toString()), json)
                } }
            }
            roots[owner].each { root ->
                fileTree(root).matching { include 'data/forestry/advancement/**', 'data/forestry/painting_variant/**', 'data/**/tags/painting_variant/**', 'assets/forestry/textures/painting/**' }.files.each { input ->
                    copyResource(root.toPath().relativize(input.toPath()).toString().replace('\\', '/'))
                }
                fileTree(root).matching { include 'data/forestry/data_maps/**/*.json', 'data/**/tags/fluid/**/*.json', 'data/forestry/flower_type/*.json', 'data/forestry/bee_effect/*.json', 'data/forestry/bee_jubilance/*.json', 'data/forestry/bee_species/*.json', 'data/forestry/tree_species/*.json', 'data/forestry/butterfly_species/*.json', 'data/forestry/taxon/*.json', 'data/forestry/damage_type/*.json' }.files.each { input ->
                    def path = root.toPath().relativize(input.toPath()).toString().replace('\\', '/')
                    def json = parser.parse(input)
                    if (path.contains('/taxon/') && json.type != null && !(json.type in (owner == 'butterflies' ? ['forestry:butterfly_species'] : ['forestry:bee_species', 'forestry:tree_species']))) return
                    // NeoForge supplies minecraft:milk. Fabric exposes milk through the conventional fluid tag.
                    if (path.endsWith('/biogas_fuels.json') && json.values.containsKey('minecraft:milk')) {
                        json.values['#c:milk'] = [value: json.values.remove('minecraft:milk'), required: false]
                    }
                    writeJson(new File(target, path), json)
                }
            }
            int recipes = 0
            int tags = 0
            def deferred = []
            def resources = new TreeMap<String, File>()
            roots[owner].each { root -> fileTree(root).matching { include 'data/**/recipe/**/*.json', 'data/**/tags/item/**/*.json', 'data/**/tags/block/**/*.json' }.files.each { input ->
                resources[root.toPath().relativize(input.toPath()).toString().replace('\\', '/')] = input
            } }
            def convertCondition
            convertCondition = { Map condition ->
                switch (condition.type) {
                    case 'neoforge:tag_empty': return [condition: 'fabric:not', value: [condition: 'fabric:tags_populated', registry: 'minecraft:item', values: [condition.tag]]]
                    case 'neoforge:not': return [condition: 'fabric:not', value: convertCondition(condition.value)]
                    case 'neoforge:and': return [condition: 'fabric:and', values: condition.values.collect { convertCondition(it) }]
                    case 'neoforge:or': return [condition: 'fabric:or', values: condition.values.collect { convertCondition(it) }]
                    case 'neoforge:mod_loaded': return [condition: 'fabric:all_mods_loaded', values: [condition.modid]]
                    default: throw new IllegalStateException("Unsupported recipe condition: ${condition}")
                }
            }
            def convertIngredient
            convertIngredient = { Object node ->
                if (node instanceof List) return node.collect { convertIngredient(it) }
                if (!(node instanceof Map)) return node
                if (node['neoforge:ingredient_type'] == 'neoforge:compound')
                    return ['fabric:type': 'fabric:any', ingredients: node.children.collect { convertIngredient(it) }]
                if (node['neoforge:ingredient_type'] == 'neoforge:components') {
                    if (!node.strict || !(node.items instanceof String) || node.items.startsWith('#'))
                        throw new IllegalStateException("Unsupported component ingredient: ${node}")
                    return ['fabric:type': 'forestry:exact_components', stack: [id: node.items, components: node.components]]
                }
                def converted = node.collectEntries { key, value -> [(key): convertIngredient(value)] }
                if (converted['neoforge:ingredient_type'] == 'forestry:fluid_container') {
                    converted.remove('neoforge:ingredient_type'); converted['fabric:type'] = 'forestry:fluid_container'
                }
                converted
            }
            // Component/genome IDs in an item stack are not item IDs. Only inspect recipe input/output structure.
            def itemReferences
            itemReferences = { Object node ->
                if (node instanceof List) return node.collectMany { itemReferences(it) }
                if (node instanceof Map) return node.findAll { key, value -> !(key in ['tag', 'components', 'fabric:load_conditions', 'fabric:type', 'type']) }.values().collectMany { itemReferences(it) }
                node instanceof String ? [node] : []
            }
            resources.each { path, input ->
                def json = parser.parse(input)
                if (path.contains('/tags/')) {
                    def allowed = path.contains('/tags/block/') ? allowedBlocks : allowedItems
                    def values = json.values.findAll { value ->
                        def id = value instanceof Map ? value.id : value
                        id.startsWith('#') || id.startsWith('minecraft:') || allowed.contains(id)
                    }.collect { value ->
                        def id = value instanceof Map ? value.id : value
                        // Deferred optional tag branches must not invalidate the entire active tag.
                        id.startsWith('#') ? [id: id, required: false] : value
                    }
                    if (!values.isEmpty()) { writeJson(new File(target, path), [replace: json.replace ?: false, values: values]); tags++ }
                } else {
                    if (owner == 'mail' && path ==~ /data\/forestry\/recipe\/stamp_.*\.json/) {
                        if (json.key?.Z instanceof Map && json.key.Z['neoforge:ingredient_type'] == 'neoforge:compound') {
                            json.key.Z = '#forestry:stamp_adhesives'
                        }
                    }
                    if (json.containsKey('neoforge:conditions')) json['fabric:load_conditions'] = json.remove('neoforge:conditions').collect { convertCondition(it) }
                    json = convertIngredient(json)
                    def references = itemReferences(json)
                    def machineTypes = ['carpenter', 'centrifuge', 'fabricator', 'fabricator_smelting', 'fermenter', 'moistener', 'smelter', 'squeezer', 'squeezer_container', 'still', 'hygroregulator'].collect { "forestry:${it}".toString() } as Set
                    def fluids = ['ethanol', 'biomass', 'liquid_glass', 'honey', 'crushed_ice', 'fruit_juice', 'seed_oil', 'short_mead', 'wax', 'experience'].collect { "forestry:${it}".toString() } as Set
                    boolean machine = machineTypes.contains(json.type) && !JsonOutput.toJson(json).contains('neoforge:') &&
                        references.findAll { it.startsWith('forestry:') && it != json.id && it != json.type }.every { allowedItems.contains(it) || fluids.contains(it) }
                    boolean eligible = machine || json.type in ['forestry:bee_mutation', 'forestry:tree_mutation', 'forestry:butterfly_mutation', 'forestry:butterfly_mating'] || json.type?.startsWith('minecraft:') && !JsonOutput.toJson(json).contains('neoforge:') &&
                        (references.any { allowedItems.contains(it) || it.startsWith('#forestry:') || it == '#c:storage_blocks/charcoal' } || json.containsKey('fabric:load_conditions')) &&
                        references.findAll { it.startsWith('forestry:') }.every { allowedItems.contains(it) }
                    if (eligible) { writeJson(new File(target, path), json); recipes++ }
                    else deferred.add(path)
                }
            }
            if (owner == 'mail') writeJson(new File(target, 'data/forestry/tags/item/stamp_adhesives.json'),
                [values: ['#forestry:drop_honey', 'minecraft:slime_ball']])
            report[owner] = [items: scope[owner].items.size(), blocks: scope[owner].blocks.size(), recipes: recipes, tags: tags,
                copiedAssets: copied.size(), deferredRecipes: deferred]
        }
        writeJson(new File(outputRoot, 'resource-report.json'), report)
        println('F3 resources: ' + report.collectEntries { owner, details -> [(owner): [items: details.items, blocks: details.blocks, recipes: details.recipes]] })
    }

    private File file(String path) {
        new File(projectRoot, path)
    }

    private static ResourceTree fileTree(Object root) {
        new ResourceTree(root instanceof File ? root.toPath() : (Path) root)
    }

    private static final class ResourceTree {
        private final Path root
        private final List<Pattern> patterns = []

        ResourceTree(Path root) { this.root = root }

        ResourceTree matching(Closure filter) {
            Closure configured = filter.rehydrate(this, this, this)
            configured.resolveStrategy = Closure.DELEGATE_ONLY
            configured.call()
            this
        }

        void include(String... globs) {
            globs.each { glob ->
                String expression = glob.split('/', -1).collect { segment ->
                    segment == '**' ? '(?:[^/]+/)*' : Pattern.quote(segment).replace('*', '\\E[^/]*\\Q') + '/'
                }.join('')
                if (!glob.endsWith('**')) expression = expression.substring(0, expression.length() - 1)
                else expression = expression.substring(0, expression.length() - '(?:[^/]+/)*'.length()) + '.*'
                patterns.add(Pattern.compile('^' + expression + '$'))
            }
        }

        List<File> getFiles() {
            if (!Files.isDirectory(root)) return []
            new ArrayList<File>(Files.walk(root).withCloseable { paths ->
                paths.filter { Files.isRegularFile(it) }.filter { path ->
                    String relative = root.relativize(path).toString().replace('\\', '/')
                    patterns.any { it.matcher(relative).matches() }
                }.sorted().map { it.toFile() }.toList()
            })
        }
    }
}
