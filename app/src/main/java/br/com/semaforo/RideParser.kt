package br.com.semaforo

data class Offer(val price: Double, val km: Double, val min: Double)

object RideParser {
    private val priceRx = Regex("""R\$\s*(\d{1,3}(?:\.\d{3})*|\d+),(\d{2})""")
    private val minRx = Regex("""(\d+)\s*min""", RegexOption.IGNORE_CASE)
    private val kmRx = Regex("""(\d+(?:[.,]\d+)?)\s*km""", RegexOption.IGNORE_CASE)

    /**
     * Regra: preço = primeiro "R$ x,xx" da tela.
     * Distância e tempo = soma dos 2 primeiros km / min (ida até o passageiro + viagem).
     * Se só houver 1 de cada, usa esse.
     */
    fun parse(texts: List<String>): Offer? {
        val all = texts.joinToString(" | ")
        val pm = priceRx.find(all) ?: return null
        val price = (pm.groupValues[1].replace(".", "") + "." + pm.groupValues[2]).toDoubleOrNull()
            ?: return null
        val kms = kmRx.findAll(all).mapNotNull { it.groupValues[1].replace(',', '.').toDoubleOrNull() }.toList()
        val mins = minRx.findAll(all).mapNotNull { it.groupValues[1].toDoubleOrNull() }.toList()
        if (kms.isEmpty() || mins.isEmpty()) return null
        val km = kms.take(2).sum()
        val min = mins.take(2).sum()
        if (km <= 0.0 || min <= 0.0 || price <= 0.0) return null
        return Offer(price, km, min)
    }
}
