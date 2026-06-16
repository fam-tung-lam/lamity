package com.phamtunglam.lamity.feature.llmModels.domain

/** Built-in model catalog (verified .litertlm files from the HuggingFace litert-community). */
object ModelCatalog {
    val seed: List<LlmModel> =
        listOf(
            LlmModel(
                id = "qwen2.5-1.5b-instruct-q8",
                name = "Qwen2.5 1.5B Instruct",
                description =
                    "Solid all-round chat model with good tool-calling support. " +
                        "Quantized to int8. No HuggingFace token required.",
                url =
                    "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/" +
                        "19edb84c69a0212f29a6ef17ba0d6f278b6a1614/" +
                        "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm?download=true",
                fileName = "Qwen2.5-1.5B-Instruct_q8_ekv4096.litertlm",
                sizeBytes = 1_597_931_520,
                requiresAuth = false,
                learnMoreUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct",
                config = ModelConfig(LlmBackend.GPU, 4096, 20, 0.8, 0.7),
            ),
            LlmModel(
                id = "gemma3-1b-it-int4",
                name = "Gemma 3 1B IT",
                description =
                    "Google's compact Gemma 3 instruction-tuned model, int4. " +
                        "Small and fast. Requires a HuggingFace access token (gated license).",
                url =
                    "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/" +
                        "42d538a932e8d5b12e6b3b455f5572560bd60b2c/gemma3-1b-it-int4.litertlm?download=true",
                fileName = "gemma3-1b-it-int4.litertlm",
                sizeBytes = 584_417_280,
                requiresAuth = true,
                learnMoreUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
                config = ModelConfig(LlmBackend.GPU, 1024, 64, 0.95, 1.0),
            ),
            LlmModel(
                id = "gemma-4-e2b-it",
                name = "Gemma 4 E2B (2B)",
                description =
                    "Google's Gemma 4 with 2B effective parameters. Strong quality and " +
                        "tool calling for its size. Large download. No HuggingFace token required.",
                url =
                    "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/" +
                        "361a4010ad6d88fc5c86e148e333c0342b99763d/gemma-4-E2B-it.litertlm?download=true",
                fileName = "gemma-4-E2B-it.litertlm",
                sizeBytes = 2_588_147_712,
                requiresAuth = false,
                learnMoreUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm",
                config = ModelConfig(LlmBackend.GPU, 4096, 64, 0.95, 1.0),
            ),
            LlmModel(
                id = "gemma-4-e4b-it",
                name = "Gemma 4 E4B (4B)",
                description =
                    "Google's Gemma 4 with 4B effective parameters. Excellent quality " +
                        "and tool calling. Needs a high-end device. No HuggingFace token required.",
                url =
                    "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/" +
                        "f7ad3343bd6ebc9607f4dc3bc4f2398bd5749bc5/gemma-4-E4B-it.litertlm?download=true",
                fileName = "gemma-4-E4B-it.litertlm",
                sizeBytes = 3_659_530_240,
                requiresAuth = false,
                learnMoreUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm",
                config = ModelConfig(LlmBackend.GPU, 4096, 64, 0.95, 1.0),
            ),
            LlmModel(
                id = "gemma-4-12b-it",
                name = "Gemma 4 12B",
                description =
                    "Google's largest on-device Gemma 4 — the strongest model in this " +
                        "catalog. 6.5 GB download; only for flagship devices with 12 GB+ RAM. " +
                        "No HuggingFace token required.",
                url =
                    "https://huggingface.co/litert-community/gemma-4-12B-it-litert-lm/resolve/" +
                        "44cf85a326f79b814fa86a60af414c042755b43a/gemma-4-12B-it.litertlm?download=true",
                fileName = "gemma-4-12B-it.litertlm",
                sizeBytes = 6_547_589_312,
                requiresAuth = false,
                learnMoreUrl = "https://huggingface.co/litert-community/gemma-4-12B-it-litert-lm",
                config = ModelConfig(LlmBackend.GPU, 4096, 64, 0.95, 1.0),
            ),
            LlmModel(
                id = "deepseek-r1-distill-qwen-1.5b-q8",
                name = "DeepSeek R1 Distill 1.5B",
                description =
                    "Reasoning model that streams its thinking before answering. " +
                        "Quantized to int8. No HuggingFace token required.",
                url =
                    "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/" +
                        "e34bb88632342d1f9640bad579a45134eb1cf988/" +
                        "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm?download=true",
                fileName = "DeepSeek-R1-Distill-Qwen-1.5B_q8_ekv4096.litertlm",
                sizeBytes = 1_833_451_520,
                requiresAuth = false,
                supportsThinking = true,
                supportsTools = false,
                learnMoreUrl = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B",
                config = ModelConfig(LlmBackend.GPU, 4096, 64, 0.95, 1.0),
            ),
        )
}
