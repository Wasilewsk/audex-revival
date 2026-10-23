package com.infinity.audex_revivel
import com.infinity.audex_revivel.core.AudexEvent
import com.infinity.audex_revivel.core.GestureFeedback
import org.junit.Assert.*
import org.junit.Test
class GestureFeedbackTest {
 @Test fun readerWithoutDetectionEvents() {
  val f = GestureFeedback()
  repeat(3) { assertEquals(AudexEvent.GESTURE_BEGIN, f.touchStarted()); assertEquals(AudexEvent.GESTURE_END, f.touchEnded()) }
 }
 @Test fun nestedDetectionDoesNotDuplicateSounds() {
  val f = GestureFeedback()
  assertEquals(AudexEvent.GESTURE_BEGIN, f.touchStarted())
  assertNull(f.detectionStarted())
  assertEquals(AudexEvent.GESTURE_END, f.detectionEnded())
  assertNull(f.touchEnded()); assertNull(f.detectionEnded())
 }
 @Test fun detectionOnlyStillWorks() {
  val f = GestureFeedback()
  repeat(2) { assertEquals(AudexEvent.GESTURE_BEGIN, f.detectionStarted()); assertEquals(AudexEvent.GESTURE_END, f.detectionEnded()) }
 }
 @Test fun reversedEndOrderDoesNotDuplicate() {
  val f = GestureFeedback(); f.touchStarted(); f.detectionStarted()
  assertEquals(AudexEvent.GESTURE_END, f.touchEnded()); assertNull(f.detectionEnded())
 }
 @Test fun interruptionClearsState() {
  val f = GestureFeedback(); f.touchStarted(); f.reset(); assertNull(f.touchEnded())
  assertEquals(AudexEvent.GESTURE_BEGIN, f.detectionStarted())
 }
}
