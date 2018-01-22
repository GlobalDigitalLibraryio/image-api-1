/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model

import com.sksamuel.elastic4s.analyzers._
import io.digitallibrary.language.model.LanguageTag
import no.ndla.imageapi.model.domain.LanguageField

object Language {
  val DefaultLanguage = LanguageTag("eng")
  val UnknownLanguage = "unknown"
  val AllLanguages = "all"
  val NoLanguage = ""

  val languageAnalyzers = Seq(
    LanguageAnalyzer("nob", NorwegianLanguageAnalyzer),
    LanguageAnalyzer("eng", EnglishLanguageAnalyzer),
    LanguageAnalyzer("fra", FrenchLanguageAnalyzer),
    LanguageAnalyzer("deu", GermanLanguageAnalyzer),
    LanguageAnalyzer("spa", SpanishLanguageAnalyzer),
    LanguageAnalyzer("sme", StandardAnalyzer), // SAMI
    LanguageAnalyzer("zho", ChineseLanguageAnalyzer),
    LanguageAnalyzer(UnknownLanguage, EnglishLanguageAnalyzer)
  )

  val supportedLanguages = languageAnalyzers.map(_.lang)

  def findByLanguageOrBestEffort[P <: LanguageField[_]](sequence: Seq[P], lang: LanguageTag): Option[P] = {
    def findFirstLanguageMatching(sequence: Seq[P], lang: Seq[LanguageTag]): Option[P] = {
      lang match {
        case Nil => sequence.headOption
        case head :: tail =>
          sequence.find(_.language == head) match {
            case Some(x) => Some(x)
            case None => findFirstLanguageMatching(sequence, tail)
          }
      }
    }

    findFirstLanguageMatching(sequence, Seq(lang, DefaultLanguage))
  }

  def languageOrUnknown(language: Option[String]): String = {
    language.filter(_.nonEmpty) match {
      case Some(x) => x
      case None => UnknownLanguage
    }
  }
}

case class LanguageAnalyzer(lang: String, analyzer: Analyzer)

